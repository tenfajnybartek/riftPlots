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
    private final PlotManager plotManager;
    private final VaultHook vaultHook;
    private final NotificationService notificationService;
    private final CityBudgetService budgetService;
    private final AuditService auditService;
    private final TransactionRepository transactionRepository;
    private final pl.riftplots.config.PluginConfiguration config;
    private final pl.riftplots.config.MessageConfiguration messageConfig;

    private final Map<String, TransferOffer> activeOffers = new ConcurrentHashMap<>();

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
        if (plot == null || seller == null || buyer == null || price == null || price.signum() < 0
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
                || !plotManager.validateAcquisition(plot, buyer.getUniqueId()).isValid()) {
            seller.sendMessage(messageConfig.get("transfer.not-ready"));
            return Optional.empty();
        }

        int duration = validSeconds > 0 ? validSeconds : config.getTransferOfferDurationSeconds();
        activeOffers.values().removeIf(offer -> offer.isExpired()
                || offer.getPlotId().equals(plot.getId()));
        TransferOffer offer = TransferOffer.create(
                plot.getId(), seller.getUniqueId(), seller.getName(),
                buyer.getUniqueId(), buyer.getName(), price, duration
        );
        activeOffers.put(offer.getId(), offer);

        notificationService.notifyPlayer(seller.getUniqueId(), messageConfig.getRaw("transfer.offer-sent", "{prefix}<green>Wyslano oferte!</green>")
                .replace("{prefix}", config.getPrefix())
                .replace("{name}", plot.getName())
                .replace("{buyer}", buyer.getName())
                .replace("{price}", config.formatMoney(price)), "minecraft:block.note_block.pling");

        notificationService.notifyPlayer(buyer.getUniqueId(), messageConfig.getRaw("transfer.offer-received", "{prefix}<yellow>Otrzymales oferte zakupu!</yellow>")
                .replace("{prefix}", config.getPrefix())
                .replace("{name}", plot.getName())
                .replace("{seller}", seller.getName())
                .replace("{price}", config.formatMoney(price))
                .replace("{offer_id}", offer.getId()), "minecraft:entity.experience_orb.pickup");

        auditService.log(plot.getId(), seller.getUniqueId(), seller.getName(), "OFFER_CREATED", "Cena: " + price + ", Kupujący: " + buyer.getName());
        return Optional.of(offer);
    }

    public synchronized boolean acceptOffer(Player buyer, String offerId) {
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

            BigDecimal feePercent = BigDecimal.valueOf(config.getTransferFeePercent() / 100.0);
            BigDecimal adminFee = price.multiply(feePercent).setScale(2, RoundingMode.HALF_UP);
            BigDecimal sellerPayout = price.subtract(adminFee);

            org.bukkit.OfflinePlayer seller = org.bukkit.Bukkit.getOfflinePlayer(offer.getSellerUuid());
            if (!vaultHook.deposit(seller, sellerPayout)) {
                if (!vaultHook.deposit(buyer, price)) {
                    throw new IllegalStateException("Nie udało się zwrócić środków za ofertę " + offerId);
                }
                buyer.sendMessage(messageConfig.get("general.economy-error"));
                return false;
            }

            if (adminFee.compareTo(BigDecimal.ZERO) > 0) {
                budgetService.recordTransaction(CityTransactionType.PURCHASE_INCOME, adminFee, plot.getId(), buyer.getUniqueId(), "Prowizja administracyjna z transferu działki");
            }

            transactionRepository.save(TransactionRecord.create(plot.getId(), buyer.getUniqueId(), buyer.getName(), TransactionType.PURCHASE, price, TransactionStatus.SUCCESS, "Zakup na rynku wtórnym od " + offer.getSellerName()));
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

        notificationService.notifyPlayer(buyer.getUniqueId(), messageConfig.getRaw("transfer.buyer-success", "{prefix}<green>Przejales wlasnosc dzialki!</green>")
                .replace("{prefix}", config.getPrefix())
                .replace("{name}", plot.getName()), "minecraft:ui.toast.challenge_complete");

        notificationService.notifyPlayer(offer.getSellerUuid(), messageConfig.getRaw("transfer.seller-success", "{prefix}<green>Dzialka zostala sprzedana!</green>")
                .replace("{prefix}", config.getPrefix())
                .replace("{name}", plot.getName())
                .replace("{buyer}", buyer.getName())
                .replace("{price}", config.formatMoney(price)), "minecraft:entity.player.levelup");

        auditService.log(plot.getId(), buyer.getUniqueId(), buyer.getName(), "TRANSFER_COMPLETED", "Sprzedający: " + offer.getSellerName() + ", Kwota: " + price);
        return true;
    }

    public synchronized void cancelOffer(Player seller, String offerId) {
        TransferOffer offer = activeOffers.get(offerId);
        if (offer != null && (offer.getSellerUuid().equals(seller.getUniqueId())
                || offer.getBuyerUuid().equals(seller.getUniqueId()))) {
            offer.setStatus(TransferStatus.CANCELLED);
            activeOffers.remove(offerId);
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
