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

    public AdminMigrateSubCommand(DataMigrationService migrationService) {
        this.migrationService = migrationService;
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

        // Run on the server thread so no command or tax tick can change the source mid-copy.
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
