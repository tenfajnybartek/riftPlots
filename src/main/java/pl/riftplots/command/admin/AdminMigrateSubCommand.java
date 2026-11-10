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
        return "plots admin migrate <źródło: YAML|SQLITE|MYSQL> <cel: YAML|SQLITE|MYSQL>";
    }

    @Override
    public String getDescription() {
        return "Przeprowadza migrację danych między silnikami baz danych.";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (plugin != null && args.length == 2 && args[0].equalsIgnoreCase("resume") && args[1].equalsIgnoreCase("confirm")) {
            if (plugin.getIoQueue().pending() > 0) {
                sender.sendMessage("Kopiowanie jeszcze trwa.");
                return true;
            }
            try {
                var file = new java.io.File(plugin.getDataFolder(), "storage.yml");
                var settings = pl.riftplots.storage.yaml.YamlDataFile.load(file);
                settings.set("storage.type", migrationService.getActiveStorageType().name());
                pl.riftplots.storage.yaml.YamlDataFile.save(settings, file);
                plugin.getMaintenance().leave();
                sender.sendMessage("Wznowiono działanie starego magazynu. Skopiowany cel przestaje być aktualny; nie przełączaj się na niego bez nowej migracji.");
            } catch (Exception e) {
                sender.sendMessage("Nie można wznowić pracy. Sprawdź log serwera.");
                plugin.getLogger().log(java.util.logging.Level.SEVERE, "Nie można przerwać migracji", e);
            }
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("§cUżycie: /" + getUsage());
            return true;
        }

        StorageType src;
        StorageType dst;
        try {
            src = StorageType.valueOf(args[0].toUpperCase(Locale.ROOT));
            dst = StorageType.valueOf(args[1].toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            sender.sendMessage("§cNieprawidłowy typ magazynu danych (YAML, SQLITE, MYSQL).");
            return true;
        }

        if (src == dst) {
            sender.sendMessage("§cMagazyn źródłowy i docelowy nie mogą być takie same!");
            return true;
        }
        if (src != migrationService.getActiveStorageType()) {
            sender.sendMessage("§cŹródłem musi być aktualnie używany magazyn: " + migrationService.getActiveStorageType());
            return true;
        }

        AdventureUtil.sendMessage(sender, "<yellow>Rozpoczynanie migracji danych z <gold>" + src + "</gold> do <gold>" + dst + "</gold>...</yellow>");

        if (plugin != null) {
            if (plugin.getPlotManager().getAllPlots().stream().anyMatch(plot -> plot.getStatus().isResetting() || plugin.getResetService().isResetting(plot.getId()))) {
                sender.sendMessage("Poczekaj na zakończenie resetów przed migracją.");
                return true;
            }
            try {
                plugin.getMaintenance().enter();
            } catch (IllegalStateException e) {
                sender.sendMessage(e.getMessage());
                return true;
            }
            sender.sendMessage("Kopiowanie działa w tle. Zmiany i nowe logowania są wstrzymane do przełączenia magazynu.");
            migrationService.migrateAsync(src, dst, plugin.getIoQueue()).whenComplete((report, error) -> {
                if (error != null) plugin.getMaintenance().leave();
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (error != null) {
                        sender.sendMessage("Migracja nie powiodła się. Stary magazyn działa dalej; sprawdź log i częściowo zapisany cel.");
                        plugin.getLogger().log(java.util.logging.Level.SEVERE, "Błąd migracji", error);
                    } else {
                        try {
                            var file = new java.io.File(plugin.getDataFolder(), "storage.yml");
                            var settings = pl.riftplots.storage.yaml.YamlDataFile.load(file);
                            settings.set("storage.type", dst.name());
                            pl.riftplots.storage.yaml.YamlDataFile.save(settings, file);
                            sender.sendMessage("Skopiowano " + report.plotsMigrated() + " działek. Ustawiono magazyn docelowy w storage.yml. Zrestartuj serwer; zmiany pozostają wstrzymane.");
                        } catch (Exception e) {
                            sender.sendMessage("Dane skopiowano, ale zapis storage.yml nie powiódł się. Pozostawiono wstrzymanie zmian.");
                            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Nie można ustawić magazynu docelowego", e);
                        }
                    }
                });
            });
            return true;
        }

        try {
            DataMigrationService.MigrationReport report = migrationService.migrate(src, dst);
            AdventureUtil.sendMessage(sender, "<green><b>Migracja zakończona sukcesem!</b></green>");
            AdventureUtil.sendMessage(sender, "<gray>Przeniesiono działek: </gray><yellow>" + report.plotsMigrated() + "</yellow>");
            AdventureUtil.sendMessage(sender, "<gray>Przeniesiono transakcji: </gray><yellow>" + report.transactionsMigrated() + "</yellow>");
            AdventureUtil.sendMessage(sender, "<gray>Przeniesiono tabliczek: </gray><yellow>" + report.signsMigrated() + "</yellow>");
            AdventureUtil.sendMessage(sender, "<gold>Aby zacząć korzystać z nowej bazy, zmień storage.type w storage.yml i zrestartuj serwer.</gold>");
        } catch (Exception e) {
            AdventureUtil.sendMessage(sender, "<red>Błąd podczas migracji danych: " + e.getMessage() + "</red>");
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
