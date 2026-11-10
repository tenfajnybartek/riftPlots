package pl.riftplots.service.transfer;

import org.bukkit.entity.Player;
import pl.riftplots.domain.city.CityTransactionType;
import pl.riftplots.domain.payment.TransactionRecord;
import pl.riftplots.domain.payment.TransactionStatus;
import pl.riftplots.domain.payment.TransactionType;
import pl.riftplots.domain.plot.Plot;
import pl.riftplots.domain.plot.PlotStatus;
import pl.riftplots.domain.transfer.TransferOffer;
import pl.riftplots.domain.transfer.TransferStatus;
import pl.riftplots.integration.vault.VaultHook;
import pl.riftplots.service.audit.AuditService;
import pl.riftplots.service.city.CityBudgetService;
import pl.riftplots.service.notification.NotificationService;
import pl.riftplots.service.plot.PlotManager;
import pl.riftplots.storage.repository.TransactionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlotTransferService {
    private pl.riftplots.service.transaction.OperationJournal journal;

    public void setOperationJournal(pl.riftplots.service.transaction.OperationJournal journal) {
        this.journal = journal;
    }

    private final PlotManager plotManager;
    private final VaultHook vaultHook;
    private final NotificationService notificationService;
    private final CityBudgetService budgetService;
    private final AuditService auditService;
    private final TransactionRepository transactionRepository;
    private final pl.riftplots.config.PluginConfiguration config;
    private final pl.riftplots.config.MessageConfiguration messageConfig;

    private final Map<String, TransferOffer> activeOffers = new ConcurrentHashMap<>();
    private java.io.File offersFile;

    public synchronized void initialize(java.io.File dataFolder) {
        offersFile = new java.io.File(dataFolder, "data/transfers.yml");
        var yaml = pl.riftplots.storage.yaml.YamlDataFile.load(offersFile);
        var section = yaml.getConfigurationSection("offers");
        activeOffers.clear();
        if (section == null) return;
        for (String id : section.getKeys(false)) {
            var row = section.getConfigurationSection(id);
            var offer = new TransferOffer(id, row.getString("plot"), UUID.fromString(row.getString("seller")), row.getString("seller-name"),
                    UUID.fromString(row.getString("buyer")), row.getString("buyer-name"), new BigDecimal(row.getString("price")),
                    Instant.ofEpochMilli(row.getLong("created")), Instant.ofEpochMilli(row.getLong("expires")), TransferStatus.PENDING);
            if (!offer.isExpired()) activeOffers.put(id, offer);
        }
    }

    private void persistOffers() {
        if (offersFile == null) return;
        var yaml = new org.bukkit.configuration.file.YamlConfiguration();
        yaml.set("format", 1);
        for (var offer : activeOffers.values()) {
            if (offer.isExpired() || offer.getStatus() != TransferStatus.PENDING) continue;
            String path = "offers." + offer.getId();
            yaml.set(path + ".plot", offer.getPlotId());
            yaml.set(path + ".seller", offer.getSellerUuid().toString());
            yaml.set(path + ".seller-name", offer.getSellerName());
            yaml.set(path + ".buyer", offer.getBuyerUuid().toString());
            yaml.set(path + ".buyer-name", offer.getBuyerName());
            yaml.set(path + ".price", offer.getPrice().toPlainString());
            yaml.set(path + ".created", offer.getCreatedAt().toEpochMilli());
            yaml.set(path + ".expires", offer.getExpiresAt().toEpochMilli());
        }
        try {
            pl.riftplots.storage.yaml.YamlDataFile.save(yaml, offersFile);
        } catch (java.io.IOException e) {
            throw new IllegalStateException(pl.riftplots.config.Texts.raw("text.plot-transfer-service.nie-mozna-zapisac-ofert-transferu"), e);
        }
    }

    public PlotTransferService(PlotManager plotManager,
                               VaultHook vaultHook,
                               NotificationService notificationService,
                               CityBudgetService budgetService,
                               AuditService auditService,
                               TransactionRepository transactionRepository,
                               pl.riftplots.config.PluginConfiguration config,
                               pl.riftplots.config.MessageConfiguration messageConfig) {
        this.plotManager = plotManager;
        this.vaultHook = vaultHook;
        this.notificationService = notificationService;
        this.budgetService = budgetService;
        this.auditService = auditService;
        this.transactionRepository = transactionRepository;
        this.config = config;
        this.messageConfig = messageConfig;
    }

    public synchronized Optional<TransferOffer> createOffer(Plot plot, Player seller, Player buyer, BigDecimal price, int validSeconds) {
        if (plot == null || seller == null || buyer == null || !pl.riftplots.util.Money.isSafe(price)
                || seller.getUniqueId().equals(buyer.getUniqueId())) {
            return Optional.empty();
        }

        if (!plot.isOwner(seller.getUniqueId())) {
            seller.sendMessage(messageConfig.get("transfer.not-owner"));
            return Optional.empty();
        }

        if (plot.getStatus() == PlotStatus.PAYMENT_DUE || plot.getStatus() == PlotStatus.GRACE_PERIOD) {
            seller.sendMessage(messageConfig.get("transfer.has-arrears"));
            return Optional.empty();
        }

        if (plot.getStatus() != PlotStatus.OWNED || plot.isRented() || plot.isAuctionActive()
                || journal != null && journal.isBlocked(plot.getId())
                || !plotManager.validateAcquisition(plot, buyer.getUniqueId()).isValid()) {
            seller.sendMessage(messageConfig.get("transfer.not-ready"));
            return Optional.empty();
        }

        int duration = validSeconds > 0 ? validSeconds : config.getTransferOfferDurationSeconds();
        Map<String, TransferOffer> previous = new HashMap<>(activeOffers);
        activeOffers.values().removeIf(offer -> offer.isExpired()
                || offer.getPlotId().equals(plot.getId()));
        TransferOffer offer = TransferOffer.create(
                plot.getId(), seller.getUniqueId(), seller.getName(),
                buyer.getUniqueId(), buyer.getName(), price, duration
        );
        activeOffers.put(offer.getId(), offer);
        try {
            persistOffers();
        } catch (RuntimeException e) {
            activeOffers.clear();
            activeOffers.putAll(previous);
            throw e;
        }

        notificationService.notifyPlayer(seller.getUniqueId(), messageConfig.getRaw("transfer.offer-sent")
                .replace("{prefix}", config.getPrefix())
                .replace("{name}", plot.getName())
                .replace("{buyer}", buyer.getName())
                .replace("{price}", config.formatMoney(price)), pl.riftplots.config.Settings.string("sounds.transfer-offer-sent"));

        notificationService.notifyPlayer(buyer.getUniqueId(), messageConfig.getRaw("transfer.offer-received")
                .replace("{prefix}", config.getPrefix())
                .replace("{name}", plot.getName())
                .replace("{seller}", seller.getName())
                .replace("{price}", config.formatMoney(price))
                .replace("{offer_id}", offer.getId()), pl.riftplots.config.Settings.string("sounds.transfer-offer-received"));

        auditService.log(plot.getId(), seller.getUniqueId(), seller.getName(), "OFFER_CREATED", pl.riftplots.config.Texts.raw("text.plot-transfer-service.cena-kupujacy", price, buyer.getName()));
        return Optional.of(offer);
    }

    public synchronized boolean acceptOffer(Player buyer, String offerId) {
        if (buyer == null || offerId == null) return false;
        TransferOffer offer = activeOffers.get(offerId);
        if (offer == null) return false;
        return journal == null ? acceptOfferInternal(buyer, offerId) : journal.execute("TRANSFER", "transfer:" + offerId,
                offer.getPlotId(), buyer.getUniqueId(), offer.getPrice(), () -> acceptOfferInternal(buyer, offerId));
    }

    private boolean acceptOfferInternal(Player buyer, String offerId) {
        if (buyer == null || offerId == null) return false;
        TransferOffer offer = activeOffers.get(offerId);
        if (offer == null || !offer.getBuyerUuid().equals(buyer.getUniqueId())) {
            buyer.sendMessage(messageConfig.get("transfer.offer-invalid"));
            return false;
        }

        if (offer.isExpired() || offer.getStatus() != TransferStatus.PENDING) {
            buyer.sendMessage(messageConfig.get("transfer.offer-invalid"));
            activeOffers.remove(offerId);
            return false;
        }

        Optional<Plot> optPlot = plotManager.getPlot(offer.getPlotId());
        if (optPlot.isEmpty()) {
            buyer.sendMessage(messageConfig.get("plot.not-found", Map.of("plot_id", offer.getPlotId())));
            return false;
        }
        Plot plot = optPlot.get();

        if (plot.getStatus() != PlotStatus.OWNED || plot.isRented() || plot.isAuctionActive()
                || !plotManager.validateAcquisition(plot, buyer.getUniqueId()).isValid()) {
            buyer.sendMessage(messageConfig.get("transfer.not-ready"));
            return false;
        }

        if (!plot.isOwner(offer.getSellerUuid())) {
            buyer.sendMessage(messageConfig.get("transfer.seller-changed"));
            offer.setStatus(TransferStatus.CANCELLED);
            activeOffers.remove(offerId);
            return false;
        }

        BigDecimal price = offer.getPrice();
        if (price.compareTo(BigDecimal.ZERO) > 0) {
            if (!vaultHook.has(buyer, price)) {
                buyer.sendMessage(messageConfig.get("plot.cannot-afford", Map.of("price", config.formatMoney(price), "balance", config.formatMoney(vaultHook.getBalance(buyer)))));
                return false;
            }
            if (!vaultHook.withdraw(buyer, price)) {
                return false;
            }

            BigDecimal feePercent = BigDecimal.valueOf(config.getTransferFeePercent()).movePointLeft(2);
            BigDecimal adminFee = price.multiply(feePercent).setScale(pl.riftplots.config.Settings.integer("economy.decimal-places"), pl.riftplots.config.Settings.rounding()).min(price);
            BigDecimal sellerPayout = price.subtract(adminFee);

            org.bukkit.OfflinePlayer seller = org.bukkit.Bukkit.getOfflinePlayer(offer.getSellerUuid());
            if (!vaultHook.deposit(seller, sellerPayout)) {
                if (!vaultHook.deposit(buyer, price)) {
                    throw new IllegalStateException(pl.riftplots.config.Texts.raw("text.plot-transfer-service.nie-udao-sie-zwrocic-srodkow-za-oferte", offerId));
                }
                buyer.sendMessage(messageConfig.get("general.economy-error"));
                return false;
            }

            if (adminFee.compareTo(BigDecimal.ZERO) > 0) {
                budgetService.recordTransaction(CityTransactionType.PURCHASE_INCOME, adminFee, plot.getId(), buyer.getUniqueId(), pl.riftplots.config.Texts.raw("text.plot-transfer-service.prowizja-administracyjna-z-transferu-dziaki"));
            }

            transactionRepository.save(TransactionRecord.create(plot.getId(), buyer.getUniqueId(), buyer.getName(), TransactionType.PURCHASE, price, TransactionStatus.SUCCESS, pl.riftplots.config.Texts.raw("text.plot-transfer-service.zakup-na-rynku-wtornym-od", offer.getSellerName())));
        }

        plot.setOwnerUuid(buyer.getUniqueId());
        plot.setOwnerName(buyer.getName());
        plot.setPurchaseDate(Instant.now());
        plot.clearHomeLocation();
        plot.clearMembers();
        plot.setResellAvailable(false);
        plotManager.savePlot(plot);

        offer.setStatus(TransferStatus.ACCEPTED);
        activeOffers.remove(offerId);
        activeOffers.values().removeIf(other -> other.getPlotId().equals(plot.getId()));
        persistOffers();

        notificationService.notifyPlayer(buyer.getUniqueId(), messageConfig.getRaw("transfer.buyer-success")
                .replace("{prefix}", config.getPrefix())
                .replace("{name}", plot.getName()), pl.riftplots.config.Settings.string("sounds.transfer-buyer-success"));

        notificationService.notifyPlayer(offer.getSellerUuid(), messageConfig.getRaw("transfer.seller-success")
                .replace("{prefix}", config.getPrefix())
                .replace("{name}", plot.getName())
                .replace("{buyer}", buyer.getName())
                .replace("{price}", config.formatMoney(price)), pl.riftplots.config.Settings.string("sounds.transfer-seller-success"));

        auditService.log(plot.getId(), buyer.getUniqueId(), buyer.getName(), "TRANSFER_COMPLETED", pl.riftplots.config.Texts.raw("text.plot-transfer-service.sprzedajacy-kwota", offer.getSellerName(), price));
        return true;
    }

    public synchronized void cancelOffer(Player seller, String offerId) {
        TransferOffer offer = activeOffers.get(offerId);
        if (offer != null && (offer.getSellerUuid().equals(seller.getUniqueId())
                || offer.getBuyerUuid().equals(seller.getUniqueId()))) {
            activeOffers.remove(offerId);
            try {
                persistOffers();
            } catch (RuntimeException e) {
                activeOffers.put(offerId, offer);
                throw e;
            }
            offer.setStatus(TransferStatus.CANCELLED);
            seller.sendMessage(messageConfig.get("transfer.cancelled"));
        }
    }

    public List<TransferOffer> getOffersForBuyer(UUID buyerUuid) {
        if (buyerUuid == null) return Collections.emptyList();
        return activeOffers.values().stream()
                .filter(o -> o.getBuyerUuid().equals(buyerUuid) && !o.isExpired())
                .toList();
    }
}
