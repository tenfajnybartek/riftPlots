package pl.riftplots.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import pl.riftplots.config.MessageConfiguration;
import pl.riftplots.config.PluginConfiguration;
import pl.riftplots.domain.player.PlotPlayerProfile;
import pl.riftplots.domain.plot.Plot;
import pl.riftplots.domain.plot.PlotStatus;
import pl.riftplots.service.payment.TaxCalculator;
import pl.riftplots.service.plot.PlotManager;
import pl.riftplots.storage.repository.PlayerProfileRepository;
import pl.riftplots.util.AdventureUtil;
import pl.riftplots.util.TimeUtil;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public class PlotSessionListener implements Listener {
    private final PluginConfiguration config;
    private final MessageConfiguration messageConfig;
    private final PlotManager plotManager;
    private final TaxCalculator taxCalculator;
    private final PlayerProfileRepository profileRepository;
    private pl.riftplots.service.notification.NotificationService notificationService;
    private java.util.function.BooleanSupplier writeAllowed = () -> true;

    public void setWriteAllowed(java.util.function.BooleanSupplier writeAllowed) {
        this.writeAllowed = Objects.requireNonNull(writeAllowed);
    }

    public PlotSessionListener(PluginConfiguration config,
                               MessageConfiguration messageConfig,
                               PlotManager plotManager,
                               TaxCalculator taxCalculator,
                               PlayerProfileRepository profileRepository,
                               pl.riftplots.service.notification.NotificationService notificationService) {
        this.config = config;
        this.messageConfig = messageConfig;
        this.plotManager = plotManager;
        this.taxCalculator = taxCalculator;
        this.profileRepository = profileRepository;
        this.notificationService = notificationService;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!writeAllowed.getAsBoolean()) {
            player.kick(net.kyori.adventure.text.Component.text("Trwa migracja danych miasta. Spróbuj po restarcie serwera."));
            return;
        }
        UUID uuid = player.getUniqueId();
        Instant now = Instant.now();

        Optional<PlotPlayerProfile> optProfile = profileRepository.findById(uuid);
        PlotPlayerProfile profile = optProfile.orElseGet(() -> new PlotPlayerProfile(
                uuid, player.getName(), now, now, new HashSet<>()
        ));
        profile.setLastName(player.getName());
        profile.setLastSeen(now);
        profileRepository.save(profile);

        List<Plot> ownedPlots = plotManager.getPlotsByOwner(uuid);
        for (Plot plot : ownedPlots) {
            if (plot.getStatus() == PlotStatus.GRACE_PERIOD) {
                BigDecimal tax = taxCalculator.calculateTax(plot);
                player.sendMessage(messageConfig.get("tax.grace-period-warning", Map.of(
                        "name", plot.getName(),
                        "plot_id", plot.getId(),
                        "time_left", TimeUtil.formatRemainingTime(plot.getGracePeriodExpiry()),
                        "tax", config.formatMoney(tax)
                )));
            } else if (plot.getStatus() == PlotStatus.PAYMENT_DUE) {
                BigDecimal tax = taxCalculator.calculateTax(plot);
                player.sendMessage(messageConfig.get("tax.reminder", Map.of(
                        "name", plot.getName(),
                        "plot_id", plot.getId(),
                        "time_left", "0s (termin nadszedł)",
                        "tax", config.formatMoney(tax)
                )));
            }
        }

        if (notificationService != null) {
            notificationService.deliverOfflineMessages(player);
        }
    }
}
