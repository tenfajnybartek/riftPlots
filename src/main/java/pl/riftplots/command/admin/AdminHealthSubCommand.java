package pl.riftplots.command.admin;

import org.bukkit.command.CommandSender;
import net.kyori.adventure.text.Component;
import pl.riftplots.RiftPlotsPlugin;
import pl.riftplots.command.SubCommand;
import pl.riftplots.storage.StorageBackend;
import pl.riftplots.integration.worldguard.WorldGuardHook;
import pl.riftplots.service.reset.PlotSnapshotService;

import java.util.*;

public final class AdminHealthSubCommand implements SubCommand {
    private final RiftPlotsPlugin plugin;
    private final StorageBackend backend;
    private final WorldGuardHook regions;
    private final PlotSnapshotService snapshots;

    public AdminHealthSubCommand(RiftPlotsPlugin plugin, StorageBackend backend, WorldGuardHook regions, PlotSnapshotService snapshots) {
        this.plugin = plugin;
        this.backend = backend;
        this.regions = regions;
        this.snapshots = snapshots;
    }

    public String getName() {
        return "health";
    }

    public String getPermission() {
        return "riftplots.admin.health";
    }

    public String getUsage() {
        return pl.riftplots.config.Texts.raw("text.admin-health.usage");
    }

    public String getDescription() {
        return pl.riftplots.config.Texts.raw("text.admin-health.description");
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 2 && args[0].equalsIgnoreCase("reset")) {
            var plot = plugin.getPlotManager().getPlot(args[1]).orElse(null);
            if (plot == null) {
                sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-health.dziaka-nie-istnieje"));
                return true;
            }
            plugin.getResetService().preview(plot, problem -> sender.sendMessage(Component.text(problem != null ? problem
                    : pl.riftplots.config.Texts.raw("text.admin-health.schemat-i-swiat-sa-zgodne-reset-odbierze", plot.getBounds().getVolume()))));
            return true;
        }
        sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-health.riftplots-serwer", plugin.getServer().getVersion()));
        String configured;
        try {
            configured = pl.riftplots.storage.yaml.YamlDataFile.load(new java.io.File(plugin.getDataFolder(), "storage.yml")).getString("storage.type", "SQLITE");
        } catch (RuntimeException e) {
            configured = pl.riftplots.config.Texts.raw("text.admin-health.nieczytelny-plik");
        }
        sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-health.aktywny-magazyn-storage-yml", (backend.getDatabaseManager() != null ? backend.getDatabaseManager().getStorageType() : "YAML"), configured));
        for (String name : List.of("WorldEdit", "WorldGuard", "Vault")) {
            var dependency = plugin.getServer().getPluginManager().getPlugin(name);
            sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-health.line", name, (dependency != null && dependency.isEnabled() ? dependency.getPluginMeta().getVersion() : pl.riftplots.config.Texts.raw("text.admin-health.brak-wyaczony"))));
        }
        try {
            if (backend.getDatabaseManager() != null)
                try (var connection = backend.getDatabaseManager().getConnection()) {
                    sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-health.baza-odpowiada", connection.isValid(2)));
                }
            if (backend.getDatabaseManager() != null)
                sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-health.wersja-schematu-danych", backend.getDatabaseManager().getSchemaVersion()));
        } catch (Exception e) {
            sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-health.baza-nie-odpowiada-szczegoy-sprawdz-w-logu"));
        }
        int missingSchematics = 0, missingRegions = 0, inconsistentRegions = 0;
        for (var plot : plugin.getPlotManager().getAllPlots()) {
            if (!snapshots.hasSnapshot(plot.getId())) missingSchematics++;
            var bounds = regions.getRegionBounds(plot.getBounds().getWorldName(), plot.getWorldGuardRegionId());
            if (bounds == null) missingRegions++;
            else if (!bounds.equals(plot.getBounds())) inconsistentRegions++;
        }
        sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-health.brakujace-schematy-brakujace-regiony-inne-granice-regionow", missingSchematics, missingRegions, inconsistentRegions));
        sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-health.operacje-do-uzgodnienia-oczekujace-zwroty", plugin.getOperationJournal().pending().size(), plugin.getRefundService().list(null).stream().filter(r -> r.status() == pl.riftplots.service.transaction.RefundService.Status.PENDING || r.status() == pl.riftplots.service.transaction.RefundService.Status.REVIEW_REQUIRED).count()));
        sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-health.raport-jest-tylko-do-odczytu-peny-odczyt"));
        sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-health.tryb-przeaczenia-magazynu-zadania-i-o", plugin.getMaintenance().isActive(), plugin.getIoQueue().pending()));
        return true;
    }

    public List<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 1 ? List.of("reset") : List.of();
    }
}
