package pl.riftplots.service.transfer;

import org.bukkit.Sound;
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

    private final Map<String, TransferOffer> activeOffers = new ConcurrentHashMap<>();

    public PlotTransferService(PlotManager plotManager,
                               VaultHook vaultHook,
                               NotificationService notificationService,
                               CityBudgetService budgetService,
                               AuditService auditService,
                               TransactionRepository transactionRepository) {
        this.plotManager = plotManager;
        this.vaultHook = vaultHook;
        this.notificationService = notificationService;
        this.budgetService = budgetService;
        this.auditService = auditService;
        this.transactionRepository = transactionRepository;
    }

    public synchronized Optional<TransferOffer> createOffer(Plot plot, Player seller, Player buyer, BigDecimal price, int validSeconds) {
        if (plot == null || seller == null || buyer == null) {
            return Optional.empty();
        }

        if (!plot.isOwner(seller.getUniqueId())) {
            seller.sendMessage("§cNie jesteś właścicielem tej działki.");
            return Optional.empty();
        }

        if (plot.getStatus() == PlotStatus.PAYMENT_DUE || plot.getStatus() == PlotStatus.GRACE_PERIOD) {
            seller.sendMessage("§cTa działka posiada nieuregulowane zaległości podatkowe! Opłać podatek przed sprzedażą.");
            return Optional.empty();
        }

        if (plot.getStatus() != PlotStatus.OWNED) {
            seller.sendMessage("§cDziałka nie jest w stanie gotowym do sprzedaży.");
            return Optional.empty();
        }

        TransferOffer offer = TransferOffer.create(
                plot.getId(), seller.getUniqueId(), seller.getName(),
                buyer.getUniqueId(), buyer.getName(), price, validSeconds
        );
        activeOffers.put(offer.getId(), offer);

        notificationService.notifyPlayer(seller.getUniqueId(), "<green>Wysłano ofertę sprzedaży działki <gold>" + plot.getName() + "</gold> do gracza <yellow>" + buyer.getName() + "</yellow> za kwotę <gold>" + price + " $</gold>.</green>", Sound.BLOCK_NOTE_BLOCK_PLING);
        notificationService.notifyPlayer(buyer.getUniqueId(), "<yellow>Otrzymałeś ofertę zakupu działki <gold>" + plot.getName() + "</gold> od gracza <aqua>" + seller.getName() + "</aqua> za <gold>" + price + " $</gold>! Wpisz <click:run_command:'/plots accept " + offer.getId() + "'><green><u>/plots accept " + offer.getId() + "</u></green></click> aby przyjąć.</yellow>", Sound.ENTITY_EXPERIENCE_ORB_PICKUP);

        auditService.log(plot.getId(), seller.getUniqueId(), seller.getName(), "OFFER_CREATED", "Cena: " + price + ", Kupujący: " + buyer.getName());
        return Optional.of(offer);
    }

    public synchronized boolean acceptOffer(Player buyer, String offerId) {
        if (buyer == null || offerId == null) return false;
        TransferOffer offer = activeOffers.get(offerId);
        if (offer == null || !offer.getBuyerUuid().equals(buyer.getUniqueId())) {
            buyer.sendMessage("§cTa oferta nie istnieje lub nie jest skierowana do Ciebie.");
            return false;
        }

        if (offer.isExpired() || offer.getStatus() != TransferStatus.PENDING) {
            buyer.sendMessage("§cTa oferta wygasła lub została anulowana.");
            activeOffers.remove(offerId);
            return false;
        }

        Optional<Plot> optPlot = plotManager.getPlot(offer.getPlotId());
        if (optPlot.isEmpty()) {
            buyer.sendMessage("§cPowiązana działka już nie istnieje.");
            return false;
        }
        Plot plot = optPlot.get();

        if (!plot.isOwner(offer.getSellerUuid())) {
            buyer.sendMessage("§cSprzedający nie jest już właścicielem tej działki.");
            offer.setStatus(TransferStatus.CANCELLED);
            activeOffers.remove(offerId);
            return false;
        }

        BigDecimal price = offer.getPrice();
        if (price.compareTo(BigDecimal.ZERO) > 0) {
            if (!vaultHook.has(buyer, price)) {
                buyer.sendMessage("§cNie masz wystarczających środków na zakup (" + price + " $).");
                return false;
            }
            if (!vaultHook.withdraw(buyer, price)) {
                return false;
            }

            BigDecimal feePercent = BigDecimal.valueOf(0.05);
            BigDecimal adminFee = price.multiply(feePercent).setScale(2, RoundingMode.HALF_UP);
            BigDecimal sellerPayout = price.subtract(adminFee);

            org.bukkit.OfflinePlayer seller = org.bukkit.Bukkit.getOfflinePlayer(offer.getSellerUuid());
            vaultHook.deposit(seller, sellerPayout);

            if (adminFee.compareTo(BigDecimal.ZERO) > 0) {
                budgetService.recordTransaction(CityTransactionType.PURCHASE_INCOME, adminFee, plot.getId(), buyer.getUniqueId(), "Prowizja administracyjna z transferu działki");
            }

            transactionRepository.save(TransactionRecord.create(plot.getId(), buyer.getUniqueId(), buyer.getName(), TransactionType.PURCHASE, price, TransactionStatus.SUCCESS, "Zakup na rynku wtórnym od " + offer.getSellerName()));
        }

        plot.setOwnerUuid(buyer.getUniqueId());
        plot.setOwnerName(buyer.getName());
        plot.setPurchaseDate(Instant.now());
        plot.clearHomeLocation();
        plotManager.savePlot(plot);

        offer.setStatus(TransferStatus.ACCEPTED);
        activeOffers.remove(offerId);

        notificationService.notifyPlayer(buyer.getUniqueId(), "<green>Pomyślnie przejąłeś prawo własności do działki <gold>" + plot.getName() + "</gold>!</green>", Sound.UI_TOAST_CHALLENGE_COMPLETE);
        notificationService.notifyPlayer(offer.getSellerUuid(), "<green>Twoja działka <gold>" + plot.getName() + "</gold> została sprzedana graczowi <yellow>" + buyer.getName() + "</yellow>!</green>", Sound.ENTITY_PLAYER_LEVELUP);

        auditService.log(plot.getId(), buyer.getUniqueId(), buyer.getName(), "TRANSFER_COMPLETED", "Sprzedający: " + offer.getSellerName() + ", Kwota: " + price);
        return true;
    }

    public synchronized void cancelOffer(Player seller, String offerId) {
        TransferOffer offer = activeOffers.get(offerId);
        if (offer != null && offer.getSellerUuid().equals(seller.getUniqueId())) {
            offer.setStatus(TransferStatus.CANCELLED);
            activeOffers.remove(offerId);
            seller.sendMessage("§aAnulowano ofertę sprzedaży.");
        }
    }

    public List<TransferOffer> getOffersForBuyer(UUID buyerUuid) {
        if (buyerUuid == null) return Collections.emptyList();
        return activeOffers.values().stream()
                .filter(o -> o.getBuyerUuid().equals(buyerUuid) && !o.isExpired())
                .toList();
    }
}
