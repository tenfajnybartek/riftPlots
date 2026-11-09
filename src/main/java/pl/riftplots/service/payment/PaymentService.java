package pl.riftplots.service.payment;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import pl.riftplots.api.PlotBuyEvent;
import pl.riftplots.api.PlotStateChangeEvent;
import pl.riftplots.api.PlotTaxPayEvent;
import pl.riftplots.config.MessageConfiguration;
import pl.riftplots.config.PluginConfiguration;
import pl.riftplots.domain.payment.TransactionRecord;
import pl.riftplots.domain.payment.TransactionStatus;
import pl.riftplots.domain.payment.TransactionType;
import pl.riftplots.domain.player.PlotPlayerProfile;
import pl.riftplots.domain.plot.Plot;
import pl.riftplots.domain.plot.PlotStatus;
import pl.riftplots.integration.vault.VaultHook;
import pl.riftplots.service.plot.PlotManager;
import pl.riftplots.service.reset.PlotResetService;
import pl.riftplots.service.sign.PlotSignService;
import pl.riftplots.storage.repository.PlayerProfileRepository;
import pl.riftplots.storage.repository.TransactionRepository;
import pl.riftplots.util.AdventureUtil;
import pl.riftplots.util.TimeUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PaymentService {
    private final PluginConfiguration config;
    private final MessageConfiguration messageConfig;
    private final VaultHook vaultHook;
    private final PlotManager plotManager;
    private final TaxCalculator taxCalculator;
    private final TransactionRepository transactionRepository;
    private final PlayerProfileRepository profileRepository;
    private final PlotResetService resetService;
    private PlotSignService signService;
    private pl.riftplots.service.city.CityBudgetService budgetService;
    private pl.riftplots.service.audit.AuditService auditService;

    private final Map<String, Object> plotLocks = new ConcurrentHashMap<>();

    public PaymentService(PluginConfiguration config,
                          MessageConfiguration messageConfig,
                          VaultHook vaultHook,
                          PlotManager plotManager,
                          TaxCalculator taxCalculator,
                          TransactionRepository transactionRepository,
                          PlayerProfileRepository profileRepository,
                          PlotResetService resetService) {
        this.config = config;
        this.messageConfig = messageConfig;
        this.vaultHook = vaultHook;
        this.plotManager = plotManager;
        this.taxCalculator = taxCalculator;
        this.transactionRepository = transactionRepository;
        this.profileRepository = profileRepository;
        this.resetService = resetService;
    }

    public void setSignService(PlotSignService signService) {
        this.signService = signService;
    }

    public void setBudgetService(pl.riftplots.service.city.CityBudgetService budgetService) {
        this.budgetService = budgetService;
    }

    public void setAuditService(pl.riftplots.service.audit.AuditService auditService) {
        this.auditService = auditService;
    }

    public boolean buyPlot(Player player, Plot plot) {
        if (player == null || plot == null) {
            return false;
        }

        Object lock = plotLocks.computeIfAbsent(plot.getId(), k -> new Object());
        synchronized (lock) {
            if (!plot.isAvailableForPurchase()) {
                if (plot.getStatus() == PlotStatus.RESETTING) {
                    player.sendMessage(messageConfig.get("plot.resetting", Map.of("plot_id", plot.getId())));
                } else if (plot.getStatus() == PlotStatus.DISABLED) {
                    player.sendMessage(messageConfig.get("plot.disabled", Map.of("plot_id", plot.getId())));
                } else {
                    player.sendMessage(messageConfig.get("plot.not-for-sale", Map.of(
                            "plot_id", plot.getId(),
                            "status", plot.getStatus().name()
                    )));
                }
                return false;
            }

            var validation = plotManager.validateAcquisition(plot, player.getUniqueId());
            if (!validation.isValid()) {
                player.sendMessage(net.kyori.adventure.text.Component.text(validation.getErrorMessage()));
                return false;
            }

            if (!vaultHook.isAvailable()) {
                player.sendMessage(messageConfig.get("general.economy-error"));
                return false;
            }

            BigDecimal price = plot.getPrice();
            if (!vaultHook.has(player, price)) {
                BigDecimal balance = vaultHook.getBalance(player);
                player.sendMessage(messageConfig.get("plot.cannot-afford", Map.of(
                        "price", config.formatMoney(price),
                        "balance", config.formatMoney(balance)
                )));
                return false;
            }

            PlotBuyEvent event = new PlotBuyEvent(plot, player, price);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return false;
            }

            BigDecimal finalPrice = event.getPrice();
            if (finalPrice == null || finalPrice.signum() < 0) return false;
            boolean withdrawn = vaultHook.withdraw(player, finalPrice);
            if (!withdrawn) {
                player.sendMessage(messageConfig.get("general.economy-error"));
                return false;
            }

            PlotStatus oldStatus = plot.getStatus();
            plot.setStatus(PlotStatus.OWNED);
            plot.setOwnerUuid(player.getUniqueId());
            plot.setOwnerName(player.getName());
            Instant now = Instant.now();
            plot.setPurchaseDate(now);
            plot.setNextTaxPayment(now.plus(Duration.ofDays(plot.getBillingPeriodDays())));
            plot.setGracePeriodExpiry(null);
            plot.setAdvancePeriodsPaid(0);

            plotManager.savePlot(plot);

            TransactionRecord transaction = TransactionRecord.create(
                    plot.getId(),
                    player.getUniqueId(),
                    player.getName(),
                    TransactionType.PURCHASE,
                    finalPrice,
                    TransactionStatus.SUCCESS,
                    "Zakup działki " + plot.getName()
            );
            transactionRepository.save(transaction);

            Optional<PlotPlayerProfile> optProfile = profileRepository.findById(player.getUniqueId());
            PlotPlayerProfile profile = optProfile.orElseGet(() -> new PlotPlayerProfile(
                    player.getUniqueId(), player.getName(), now, now, new HashSet<>()
            ));
            profile.setLastName(player.getName());
            profile.setLastSeen(now);
            profile.addOwnedPlot(plot.getId());
            profileRepository.save(profile);

            if (signService != null) {
                signService.updateSignsForPlot(plot);
            }

            if (budgetService != null) {
                budgetService.recordTransaction(pl.riftplots.domain.city.CityTransactionType.PURCHASE_INCOME, finalPrice, plot.getId(), player.getUniqueId(), "Zakup nieruchomości miejskiej " + plot.getId());
            }
            if (auditService != null) {
                auditService.log(plot.getId(), player.getUniqueId(), player.getName(), "PURCHASE", "Cena: " + finalPrice);
            }

            player.sendMessage(messageConfig.get("plot.buy-success", Map.of(
                    "plot_id", plot.getId(),
                    "name", plot.getName(),
                    "price", config.formatMoney(finalPrice)
            )));

            return true;
        }
    }

    public synchronized boolean payTax(Player player, Plot plot, int periods) {
        if (player == null || plot == null) {
            return false;
        }
        if (periods <= 0) {
            periods = 1;
        }
        if (!plot.getStatus().isOwned() || plot.getOwnerUuid() == null) return false;

        if (!plot.isOwner(player.getUniqueId()) && !player.hasPermission("riftplots.admin.bypass")) {
            player.sendMessage(messageConfig.get("plot.not-owner", Map.of("plot_id", plot.getId())));
            return false;
        }

        if (!plot.isTaxEnabled()) {
            player.sendMessage(AdventureUtil.parse("<gray>Ta działka jest zwolniona z podatków.</gray>"));
            return false;
        }

        if ((long) plot.getAdvancePeriodsPaid() + periods > config.getMaxAdvancePeriods()) {
            player.sendMessage(messageConfig.get("tax.max-advance-reached", Map.of("max", String.valueOf(config.getMaxAdvancePeriods()))));
            return false;
        }

        if (!vaultHook.isAvailable()) {
            player.sendMessage(messageConfig.get("general.economy-error"));
            return false;
        }

        BigDecimal singleTax = taxCalculator.calculateTax(plot);
        BigDecimal totalTax = singleTax.multiply(BigDecimal.valueOf(periods));

        if (!vaultHook.has(player, totalTax)) {
            BigDecimal balance = vaultHook.getBalance(player);
            player.sendMessage(messageConfig.get("plot.cannot-afford", Map.of(
                    "price", config.formatMoney(totalTax),
                    "balance", config.formatMoney(balance)
            )));
            return false;
        }

        PlotTaxPayEvent event = new PlotTaxPayEvent(plot, player, totalTax, periods);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }

        BigDecimal finalAmount = event.getAmount();
        if (finalAmount == null || finalAmount.signum() < 0) return false;
        boolean withdrawn = vaultHook.withdraw(player, finalAmount);
        if (!withdrawn) {
            player.sendMessage(messageConfig.get("general.economy-error"));
            return false;
        }

        Instant currentNext = plot.getNextTaxPayment();
        Instant baseTime = (currentNext != null && currentNext.isAfter(Instant.now())) ? currentNext : Instant.now();
        Instant newNext = baseTime.plus(Duration.ofDays((long) plot.getBillingPeriodDays() * periods));
        plot.setNextTaxPayment(newNext);

        if (plot.getStatus() == PlotStatus.PAYMENT_DUE || plot.getStatus() == PlotStatus.GRACE_PERIOD) {
            plot.setStatus(PlotStatus.OWNED);
            plot.setGracePeriodExpiry(null);
            plot.setAdvancePeriodsPaid(0);
        } else {
            plot.setAdvancePeriodsPaid(plot.getAdvancePeriodsPaid() + periods);
        }

        plotManager.savePlot(plot);

        TransactionRecord transaction = TransactionRecord.create(
                plot.getId(),
                player.getUniqueId(),
                player.getName(),
                periods > 1 ? TransactionType.ADVANCE_TAX : TransactionType.TAX,
                finalAmount,
                TransactionStatus.SUCCESS,
                "Opłacenie podatku za " + periods + " okres(ów)"
        );
        transactionRepository.save(transaction);

        if (signService != null) {
            signService.updateSignsForPlot(plot);
        }

        if (budgetService != null) {
            budgetService.recordTransaction(pl.riftplots.domain.city.CityTransactionType.TAX_INCOME, finalAmount, plot.getId(), player.getUniqueId(), "Wpłata podatku miejskiego za " + periods + " okres(ów)");
        }
        if (auditService != null) {
            auditService.log(plot.getId(), player.getUniqueId(), player.getName(), "TAX_PAID", "Kwota: " + finalAmount + ", Okresów: " + periods);
        }

        player.sendMessage(messageConfig.get("tax.paid-success", Map.of(
                "plot_id", plot.getId(),
                "amount", config.formatMoney(finalAmount),
                "periods", String.valueOf(periods),
                "new_date", TimeUtil.formatInstant(newNext)
        )));

        return true;
    }

    public synchronized boolean abandonPlot(Player player, Plot plot) {
        if (player == null || plot == null) {
            return false;
        }

        if (!plot.isOwner(player.getUniqueId()) && !player.hasPermission("riftplots.admin.bypass")) {
            player.sendMessage(messageConfig.get("plot.not-owner", Map.of("plot_id", plot.getId())));
            return false;
        }

        if (plot.getOwnerUuid() == null || !plot.getStatus().isOwned() || plot.isRented() || plot.isAuctionActive()) {
            player.sendMessage(messageConfig.get("transfer.not-ready"));
            return false;
        }

        BigDecimal refund = calculateAbandonRefund(plot);
        UUID ownerUuid = plot.getOwnerUuid();
        org.bukkit.OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerUuid);
        if (plot.isAutoResetOnExpire()) {
            if (!resetService.startReset(plot, null)) return false;
        } else {
            plot.clearOwnership();
            plot.setStatus(PlotStatus.AVAILABLE);
            plotManager.savePlot(plot);
        }

        if (refund.compareTo(BigDecimal.ZERO) > 0 && vaultHook.isAvailable()) {
            boolean refunded = vaultHook.deposit(owner, refund);
            TransactionRecord refundRecord = TransactionRecord.create(
                    plot.getId(),
                    ownerUuid,
                    owner.getName() != null ? owner.getName() : "Unknown",
                    TransactionType.REFUND,
                    refund,
                    refunded ? TransactionStatus.SUCCESS : TransactionStatus.FAILED,
                    "Zwrot środków za porzucenie działki"
            );
            transactionRepository.save(refundRecord);

            if (refunded && budgetService != null) {
                budgetService.recordTransaction(pl.riftplots.domain.city.CityTransactionType.REFUND_EXPENSE, refund, plot.getId(), player.getUniqueId(), "Zwrot kaucji/części ceny za porzucenie działki");
            }
            if (!refunded) {
                player.sendMessage(messageConfig.get("general.economy-error"));
                return false;
            }
        }

        if (auditService != null) {
            auditService.log(plot.getId(), player.getUniqueId(), player.getName(), "ABANDON", "Zwrot: " + refund);
        }

        Optional<PlotPlayerProfile> optProfile = profileRepository.findById(player.getUniqueId());
        optProfile.ifPresent(profile -> {
            profile.removeOwnedPlot(plot.getId());
            profileRepository.save(profile);
        });

        player.sendMessage(messageConfig.get("abandon.success", Map.of(
                "plot_id", plot.getId(),
                "refund", config.formatMoney(refund)
        )));

        return true;
    }

    public BigDecimal calculateAbandonRefund(Plot plot) {
        if (plot == null) {
            return BigDecimal.ZERO;
        }
        double percent = Math.clamp(plot.getRefundPercentOnAbandon(), 0.0, 100.0);
        if (percent <= 0.0) {
            return BigDecimal.ZERO;
        }
        BigDecimal price = plot.getPrice();
        return price.multiply(BigDecimal.valueOf(percent / 100.0)).setScale(2, RoundingMode.HALF_UP);
    }
}
