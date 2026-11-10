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
    private pl.riftplots.service.transaction.OperationJournal journal;

    public void setOperationJournal(pl.riftplots.service.transaction.OperationJournal journal) {
        this.journal = journal;
    }

    private pl.riftplots.service.transaction.RefundService refunds;

    public void setRefundService(pl.riftplots.service.transaction.RefundService refunds) {
        this.refunds = refunds;
    }

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
        if (player == null || plot == null) return false;
        return journal == null ? buyPlotInternal(player, plot) : journal.execute("PURCHASE",
                "purchase:" + plot.getId() + ":" + player.getUniqueId() + ":" + plot.getUpdatedAt().toEpochMilli(),
                plot.getId(), player.getUniqueId(), plot.getPrice(), () -> buyPlotInternal(player, plot));
    }

    private boolean buyPlotInternal(Player player, Plot plot) {
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

            BigDecimal price = Optional.ofNullable(plotManager.getPurchasePrice(plot)).orElse(plot.getPrice());
            PlotBuyEvent event = new PlotBuyEvent(plot, player, price);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return false;
            }

            BigDecimal finalPrice = event.getPrice();
            if (finalPrice == null || finalPrice.signum() < 0) return false;
            if (journal != null) journal.setCurrentAmount(finalPrice);
            if (!vaultHook.has(player, finalPrice)) {
                player.sendMessage(messageConfig.get("plot.cannot-afford", Map.of("price", config.formatMoney(finalPrice),
                        "balance", config.formatMoney(vaultHook.getBalance(player)))));
                return false;
            }
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
                    pl.riftplots.config.Texts.raw("text.payment-service.zakup-dziaki", plot.getName())
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
                budgetService.recordTransaction(pl.riftplots.domain.city.CityTransactionType.PURCHASE_INCOME, finalPrice, plot.getId(), player.getUniqueId(), pl.riftplots.config.Texts.raw("text.payment-service.zakup-nieruchomosci-miejskiej", plot.getId()));
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
        if (player == null || plot == null) return false;
        return journal == null ? payTaxInternal(player, plot, periods) : journal.execute("TAX",
                "tax:" + plot.getId() + ":" + plot.getNextTaxPayment() + ":" + periods,
                plot.getId(), player.getUniqueId(), taxCalculator.calculateTax(plot).multiply(BigDecimal.valueOf(Math.max(1, periods))),
                () -> payTaxInternal(player, plot, periods));
    }

    private boolean payTaxInternal(Player player, Plot plot, int periods) {
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
            player.sendMessage(AdventureUtil.parse(pl.riftplots.config.Texts.markup("text.payment-service.ta-dziaka-jest-zwolniona-z-podatkow")));
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

        PlotTaxPayEvent event = new PlotTaxPayEvent(plot, player, totalTax, periods);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }

        BigDecimal finalAmount = event.getAmount();
        if (finalAmount == null || finalAmount.signum() < 0) return false;
        if (journal != null) journal.setCurrentAmount(finalAmount);
        if (!vaultHook.has(player, finalAmount)) {
            player.sendMessage(messageConfig.get("plot.cannot-afford", Map.of("price", config.formatMoney(finalAmount),
                    "balance", config.formatMoney(vaultHook.getBalance(player)))));
            return false;
        }
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
                pl.riftplots.config.Texts.raw("text.payment-service.opacenie-podatku-za-okres-ow", periods)
        );
        transactionRepository.save(transaction);

        if (signService != null) {
            signService.updateSignsForPlot(plot);
        }

        if (budgetService != null) {
            budgetService.recordTransaction(pl.riftplots.domain.city.CityTransactionType.TAX_INCOME, finalAmount, plot.getId(), player.getUniqueId(), pl.riftplots.config.Texts.raw("text.payment-service.wpata-podatku-miejskiego-za-okres-ow", periods));
        }
        if (auditService != null) {
            auditService.log(plot.getId(), player.getUniqueId(), player.getName(), "TAX_PAID", pl.riftplots.config.Texts.raw("text.payment-service.kwota-okresow", finalAmount, periods));
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
        if (player == null || plot == null) return false;
        return journal == null ? abandonPlotInternal(player, plot) : journal.execute("ABANDON",
                "abandon:" + plot.getId() + ":" + plot.getOwnerUuid() + ":" + plot.getPurchaseDate(),
                plot.getId(), player.getUniqueId(), calculateAbandonRefund(plot), () -> abandonPlotInternal(player, plot));
    }

    private boolean abandonPlotInternal(Player player, Plot plot) {
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
        if (refunds != null && journal != null) {
            String operationId = journal.currentId();
            String refundKey = "abandon:" + operationId;
            refunds.prepare(refundKey, "ABANDON", plot.getId(), operationId, ownerUuid, refund, pl.riftplots.config.Texts.raw("text.payment-service.zwrot-za-porzucenie-dziaki"));
            if (plot.isAutoResetOnExpire()) {
                journal.deferCurrent();
                boolean accepted = resetService.startReset(plot, success -> {
                    boolean released = !plot.isOwner(ownerUuid);
                    journal.finishDeferred(operationId, released);
                    if (!released) refunds.cancelPrepared(refundKey);
                    refunds.retryPending();
                });
                if (!accepted) {
                    journal.finishDeferred(operationId, false);
                    refunds.cancelPrepared(refundKey);
                    return false;
                }
                player.sendMessage(AdventureUtil.parse(pl.riftplots.config.Texts.markup("text.payment-service.zlecono-oddanie-dziaki-zwrot-zostanie-rozliczony-po")));
                return true;
            }
            plot.clearOwnership();
            plot.setStatus(PlotStatus.AVAILABLE);
            plotManager.savePlot(plot);
            player.sendMessage(AdventureUtil.parse(pl.riftplots.config.Texts.markup("text.payment-service.oddano-dziake-stan-zwrotu-sprawdzisz-przez-plots")));
            return true;
        }
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
                    owner.getName() != null ? owner.getName() : pl.riftplots.config.Texts.raw("text.payment-service.unknown"),
                    TransactionType.REFUND,
                    refund,
                    refunded ? TransactionStatus.SUCCESS : TransactionStatus.FAILED,
                    pl.riftplots.config.Texts.raw("text.payment-service.zwrot-srodkow-za-porzucenie-dziaki")
            );
            transactionRepository.save(refundRecord);

            if (refunded && budgetService != null) {
                budgetService.recordTransaction(pl.riftplots.domain.city.CityTransactionType.REFUND_EXPENSE, refund, plot.getId(), player.getUniqueId(), pl.riftplots.config.Texts.raw("text.payment-service.zwrot-kaucji-czesci-ceny-za-porzucenie-dziaki"));
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
        return price.multiply(BigDecimal.valueOf(percent / 100.0)).setScale(pl.riftplots.config.Settings.integer("economy.decimal-places"), pl.riftplots.config.Settings.rounding());
    }
}
