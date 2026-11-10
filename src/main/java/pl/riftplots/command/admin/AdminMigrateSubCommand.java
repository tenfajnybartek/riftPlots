package pl.riftplots.command.admin;

import org.bukkit.command.CommandSender;
import pl.riftplots.command.SubCommand;
import pl.riftplots.config.PermissionConfiguration;
import pl.riftplots.storage.StorageType;
import pl.riftplots.storage.migration.DataMigrationService;
import pl.riftplots.util.AdventureUtil;

import java.util.*;

public class AdminMigrateSubCommand implements SubCommand {
    private final DataMigrationService migrationService;
    private pl.riftplots.RiftPlotsPlugin plugin;

    public AdminMigrateSubCommand(DataMigrationService migrationService) {
        this.migrationService = migrationService;
    }

    public AdminMigrateSubCommand(DataMigrationService migrationService, pl.riftplots.RiftPlotsPlugin plugin) {
        this(migrationService);
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "migrate";
    }

    @Override
    public String getPermission() {
        return PermissionConfiguration.ADMIN_MIGRATE;
    }

    @Override
    public String getUsage() {
        return pl.riftplots.config.Texts.raw("text.admin-migrate.usage");
    }

    @Override
    public String getDescription() {
        return pl.riftplots.config.Texts.raw("text.admin-migrate.description");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (plugin != null && args.length == 2 && args[0].equalsIgnoreCase("resume") && args[1].equalsIgnoreCase("confirm")) {
            if (plugin.getIoQueue().pending() > 0) {
                sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-migrate.kopiowanie-jeszcze-trwa"));
                return true;
            }
            try {
                var file = new java.io.File(plugin.getDataFolder(), "storage.yml");
                var settings = pl.riftplots.storage.yaml.YamlDataFile.load(file);
                settings.set("storage.type", migrationService.getActiveStorageType().name());
                pl.riftplots.storage.yaml.YamlDataFile.save(settings, file);
                plugin.getMaintenance().leave();
                sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-migrate.wznowiono-dziaanie-starego-magazynu-skopiowany-cel-przestaje"));
            } catch (Exception e) {
                sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-migrate.nie-mozna-wznowic-pracy-sprawdz-log-serwera"));
                plugin.getLogger().log(java.util.logging.Level.SEVERE, pl.riftplots.config.Texts.raw("text.admin-migrate.nie-mozna-przerwac-migracji"), e);
            }
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-migrate.uzycie", getUsage()));
            return true;
        }

        StorageType src;
        StorageType dst;
        try {
            src = StorageType.valueOf(args[0].toUpperCase(Locale.ROOT));
            dst = StorageType.valueOf(args[1].toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-migrate.nieprawidowy-typ-magazynu-danych-yaml-sqlite-mysql"));
            return true;
        }

        if (src == dst) {
            sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-migrate.magazyn-zrodowy-i-docelowy-nie-moga-byc"));
            return true;
        }
        if (src != migrationService.getActiveStorageType()) {
            sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-migrate.zrodem-musi-byc-aktualnie-uzywany-magazyn", migrationService.getActiveStorageType()));
            return true;
        }

        AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-migrate.rozpoczynanie-migracji-danych-z-do", src, dst));

        if (plugin != null) {
            if (plugin.getPlotManager().getAllPlots().stream().anyMatch(plot -> plot.getStatus().isResetting() || plugin.getResetService().isResetting(plot.getId()))) {
                sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-migrate.poczekaj-na-zakonczenie-resetow-przed-migracja"));
                return true;
            }
            try {
                plugin.getMaintenance().enter();
            } catch (IllegalStateException e) {
                sender.sendMessage(e.getMessage());
                return true;
            }
            sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-migrate.kopiowanie-dziaa-w-tle-zmiany-i-nowe"));
            migrationService.migrateAsync(src, dst, plugin.getIoQueue()).whenComplete((report, error) -> {
                if (error != null) plugin.getMaintenance().leave();
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (error != null) {
                        sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-migrate.migracja-nie-powioda-sie-stary-magazyn-dziaa"));
                        plugin.getLogger().log(java.util.logging.Level.SEVERE, pl.riftplots.config.Texts.raw("text.admin-migrate.bad-migracji"), error);
                    } else {
                        try {
                            var file = new java.io.File(plugin.getDataFolder(), "storage.yml");
                            var settings = pl.riftplots.storage.yaml.YamlDataFile.load(file);
                            settings.set("storage.type", dst.name());
                            pl.riftplots.storage.yaml.YamlDataFile.save(settings, file);
                            sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-migrate.skopiowano-dziaek-ustawiono-magazyn-docelowy-w-storage", report.plotsMigrated()));
                        } catch (Exception e) {
                            sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-migrate.dane-skopiowano-ale-zapis-storage-yml-nie"));
                            plugin.getLogger().log(java.util.logging.Level.SEVERE, pl.riftplots.config.Texts.raw("text.admin-migrate.nie-mozna-ustawic-magazynu-docelowego"), e);
                        }
                    }
                });
            });
            return true;
        }

        try {
            DataMigrationService.MigrationReport report = migrationService.migrate(src, dst);
            AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-migrate.migracja-zakonczona-sukcesem"));
            AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-migrate.przeniesiono-dziaek", report.plotsMigrated()));
            AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-migrate.przeniesiono-transakcji", report.transactionsMigrated()));
            AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-migrate.przeniesiono-tabliczek", report.signsMigrated()));
            AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-migrate.aby-zaczac-korzystac-z-nowej-bazy-zmien"));
        } catch (Exception e) {
            AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-migrate.bad-podczas-migracji-danych", e.getMessage()));
            e.printStackTrace();
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 || args.length == 2) {
            return Arrays.stream(StorageType.values()).map(Enum::name).toList();
        }
        return Collections.emptyList();
    }
}
