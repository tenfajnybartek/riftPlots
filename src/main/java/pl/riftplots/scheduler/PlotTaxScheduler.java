package pl.riftplots.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import pl.riftplots.api.PlotExpireEvent;
import pl.riftplots.config.MessageConfiguration;
import pl.riftplots.config.PluginConfiguration;
import pl.riftplots.domain.player.PlotPlayerProfile;
import pl.riftplots.domain.plot.Plot;
import pl.riftplots.domain.plot.PlotStatus;
import pl.riftplots.service.payment.TaxCalculator;
import pl.riftplots.service.plot.PlotManager;
import pl.riftplots.service.reset.PlotResetService;
import pl.riftplots.service.sign.PlotSignService;
import pl.riftplots.storage.repository.PlayerProfileRepository;
import pl.riftplots.util.AdventureUtil;
import pl.riftplots.util.TimeUtil;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class PlotTaxScheduler implements Runnable {
    private final Plugin plugin;
    private final PluginConfiguration config;
    private final MessageConfiguration messageConfig;
    private final PlotManager plotManager;
    private final TaxCalculator taxCalculator;
    private final PlotResetService resetService;
    private final PlotSignService signService;
    private final PlayerProfileRepository profileRepository;
    private pl.riftplots.service.rental.RentalService rentalService;
    private pl.riftplots.service.auction.AuctionService auctionService;

    private BukkitTask task;

    private record Reminders(Instant deadline, Set<Long> sent) {
    }

    private final Map<String, Reminders> sentReminders = new HashMap<>();
    private final Map<String, Instant> graceWarnings = new HashMap<>();
    private java.util.function.BooleanSupplier writeAllowed = () -> true;

    public void setWriteAllowed(java.util.function.BooleanSupplier allowed) {
        writeAllowed = allowed;
    }

    public PlotTaxScheduler(Plugin plugin,
                            PluginConfiguration config,
                            MessageConfiguration messageConfig,
                            PlotManager plotManager,
                            TaxCalculator taxCalculator,
                            PlotResetService resetService,
                            PlotSignService signService,
                            PlayerProfileRepository profileRepository) {
        this.plugin = plugin;
        this.config = config;
        this.messageConfig = messageConfig;
        this.plotManager = plotManager;
        this.taxCalculator = taxCalculator;
        this.resetService = resetService;
        this.signService = signService;
        this.profileRepository = profileRepository;
    }

    public void setRentalService(pl.riftplots.service.rental.RentalService rentalService) {
        this.rentalService = rentalService;
    }

    public void setAuctionService(pl.riftplots.service.auction.AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    public void start() {
        stop();
        long intervalTicks = (long) config.getSchedulerCheckIntervalSeconds() * 20L;
        this.task = Bukkit.getScheduler().runTaskTimer(plugin, this, pl.riftplots.config.Settings.longValue("performance.scheduler-initial-delay-ticks"), intervalTicks);
    }

    public void stop() {
        if (task != null && !task.isCancelled()) {
            task.cancel();
            task = null;
        }
        sentReminders.clear();
        graceWarnings.clear();
    }

    @Override
    public void run() {
        if (!writeAllowed.getAsBoolean()) return;
        Instant now = Instant.now();
        Set<String> livePlots = plotManager.getAllPlots().stream().map(Plot::getId).collect(java.util.stream.Collectors.toSet());
        sentReminders.keySet().retainAll(livePlots);
        graceWarnings.keySet().retainAll(livePlots);

        for (Plot plot : plotManager.getAllPlots()) {
            try {
                if (!plot.isTaxEnabled() || plot.getOwnerUuid() == null) {
                    continue;
                }

                if (plot.getStatus() == PlotStatus.OWNED) {
                    handleOwnedPlot(plot, now);
                } else if (plot.getStatus() == PlotStatus.GRACE_PERIOD || plot.getStatus() == PlotStatus.PAYMENT_DUE) {
                    handleGracePeriodPlot(plot, now);
                }
            } catch (RuntimeException e) {
                plugin.getLogger().log(Level.SEVERE, pl.riftplots.config.Texts.raw("text.plot-tax-scheduler.nie-udao-sie-sprawdzic-podatku-dziaki", plot.getId()), e);
            }
        }

        if (rentalService != null) {
            try {
                rentalService.checkExpiredRentals();
            } catch (RuntimeException e) {
                plugin.getLogger().log(Level.SEVERE, pl.riftplots.config.Texts.raw("text.plot-tax-scheduler.nie-udao-sie-sprawdzic-wygasych-umow-najmu"), e);
            }
        }
        if (auctionService != null) {
            try {
                auctionService.checkCompletedAuctions();
            } catch (RuntimeException e) {
                plugin.getLogger().log(Level.SEVERE, pl.riftplots.config.Texts.raw("text.plot-tax-scheduler.nie-udao-sie-sprawdzic-zakonczonych-aukcji"), e);
            }
        }
    }

    private void handleOwnedPlot(Plot plot, Instant now) {
        Instant nextTax = plot.getNextTaxPayment();
        if (nextTax == null) {
            return;
        }

        if (nextTax.isBefore(now)) {
            plot.setStatus(PlotStatus.GRACE_PERIOD);
            Instant expiry = nextTax.plus(Duration.ofHours(plot.getGracePeriodHours()));
            plot.setGracePeriodExpiry(expiry);
            plotManager.savePlot(plot);

            if (signService != null) {
                signService.updateSignsForPlot(plot);
            }

            notifyGracePeriod(plot, expiry);
            if (!expiry.isAfter(now)) expirePlot(plot);
        } else {
            checkReminders(plot, now, nextTax);
        }
    }

    private void checkReminders(Plot plot, Instant now, Instant nextTax) {
        Duration remaining = Duration.between(now, nextTax);
        long hoursRemaining = remaining.toHours();

        Reminders reminders = sentReminders.compute(plot.getId(), (id, old) -> old != null && old.deadline().equals(nextTax)
                ? old : new Reminders(nextTax, new HashSet<>()));
        config.getTaxReminderHours().stream().filter(hour -> hour > 0 && hoursRemaining <= hour).min(Long::compareTo)
                .filter(hour -> reminders.sent().add(hour)).ifPresent(hour -> notifyReminder(plot, nextTax));
    }

    private void handleGracePeriodPlot(Plot plot, Instant now) {
        Instant expiry = plot.getGracePeriodExpiry();
        if (expiry == null) {
            Instant deadline = plot.getNextTaxPayment();
            expiry = (deadline != null ? deadline : now).plus(Duration.ofHours(plot.getGracePeriodHours()));
            plot.setGracePeriodExpiry(expiry);
            plotManager.savePlot(plot);
        }

        if (!expiry.isAfter(now)) {
            expirePlot(plot);
        } else {
            notifyGracePeriod(plot, expiry);
        }
    }

    private void expirePlot(Plot plot) {
        PlotExpireEvent event = new PlotExpireEvent(plot);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }

        String ownerName = plot.getOwnerName();

        if (plot.isAutoResetOnExpire()) {
            UUID owner = plot.getOwnerUuid();
            resetService.startReset(plot, success -> {
                if (signService != null) {
                    signService.updateSignsForPlot(plot);
                }
                if (!Objects.equals(owner, plot.getOwnerUuid())) broadcastExpiration(plot, ownerName);
            });
            return;
        } else {
            if (rentalService != null && !rentalService.terminateRental(plot.getId(), true, pl.riftplots.config.Texts.raw("text.plot-tax-scheduler.wygasniecie-nieruchomosci")))
                return;
            if (auctionService != null && plot.isAuctionActive()) auctionService.cancelAuction(plot.getId());
            plot.clearOwnership();
            plot.setStatus(PlotStatus.AVAILABLE);
            plotManager.savePlot(plot);

            if (signService != null) {
                signService.updateSignsForPlot(plot);
            }
        }

        broadcastExpiration(plot, ownerName);
    }

    private void broadcastExpiration(Plot plot, String ownerName) {
        AdventureUtil.sendMessage(Bukkit.getConsoleSender(), messageConfig.getRaw("tax.expired-broadcast")
                .replace("{prefix}", config.getPrefix())
                .replace("{name}", plot.getName())
                .replace("{plot_id}", plot.getId())
                .replace("{player}", ownerName != null ? ownerName : pl.riftplots.config.Texts.raw("text.plot-tax-scheduler.nieznany")));

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.sendMessage(messageConfig.get("tax.expired-broadcast", Map.of(
                    "name", plot.getName(),
                    "plot_id", plot.getId(),
                    "player", ownerName != null ? ownerName : pl.riftplots.config.Texts.raw("text.plot-tax-scheduler.nieznany")
            )));
        }
    }

    private void notifyReminder(Plot plot, Instant nextTax) {
        if (plot.getOwnerUuid() == null) {
            return;
        }
        Player player = Bukkit.getPlayer(plot.getOwnerUuid());
        if (player != null && player.isOnline()) {
            BigDecimal tax = taxCalculator.calculateTax(plot);
            player.sendMessage(messageConfig.get("tax.reminder", Map.of(
                    "name", plot.getName(),
                    "plot_id", plot.getId(),
                    "time_left", TimeUtil.formatRemainingTime(nextTax),
                    "tax", config.formatMoney(tax)
            )));
        }
    }

    private void notifyGracePeriod(Plot plot, Instant expiry) {
        if (plot.getOwnerUuid() == null) {
            return;
        }
        Player player = Bukkit.getPlayer(plot.getOwnerUuid());
        if (player != null && player.isOnline()) {
            Instant now = Instant.now();
            Instant last = graceWarnings.get(plot.getId());
            if (last != null && last.plusSeconds(config.getGraceWarningIntervalSeconds()).isAfter(now)) return;
            graceWarnings.put(plot.getId(), now);
            BigDecimal tax = taxCalculator.calculateTax(plot);
            player.sendMessage(messageConfig.get("tax.grace-period-warning", Map.of(
                    "name", plot.getName(),
                    "plot_id", plot.getId(),
                    "time_left", TimeUtil.formatRemainingTime(expiry),
                    "tax", config.formatMoney(tax)
            )));
        }
    }
}
