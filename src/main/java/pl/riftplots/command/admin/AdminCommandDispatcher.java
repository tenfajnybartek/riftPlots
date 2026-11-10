package pl.riftplots.command.admin;

import org.bukkit.command.CommandSender;
import pl.riftplots.RiftPlotsPlugin;
import pl.riftplots.command.SubCommand;
import pl.riftplots.config.MessageConfiguration;
import pl.riftplots.config.PermissionConfiguration;
import pl.riftplots.config.PluginConfiguration;
import pl.riftplots.integration.worldedit.WorldEditHook;
import pl.riftplots.service.payment.TaxCalculator;
import pl.riftplots.service.plot.PlotManager;
import pl.riftplots.service.reset.PlotResetService;
import pl.riftplots.service.reset.PlotSnapshotService;
import pl.riftplots.service.sign.PlotSignService;
import pl.riftplots.domain.type.PropertyTypeManager;
import pl.riftplots.service.auction.AuctionService;
import pl.riftplots.service.city.CityBudgetService;
import pl.riftplots.service.district.DistrictManager;
import pl.riftplots.service.rental.RentalService;
import pl.riftplots.storage.migration.DataMigrationService;
import pl.riftplots.storage.repository.PlayerProfileRepository;
import pl.riftplots.storage.repository.TransactionRepository;
import pl.riftplots.util.AdventureUtil;

import java.util.*;

public class AdminCommandDispatcher {
    private final Map<String, SubCommand> subCommands = new HashMap<>();
    private final MessageConfiguration messageConfig;
    private java.util.function.Predicate<String> plotManagerGuard;

    public AdminCommandDispatcher(RiftPlotsPlugin plugin,
                                  PluginConfiguration config,
                                  MessageConfiguration messageConfig,
                                  PlotManager plotManager,
                                  TaxCalculator taxCalculator,
                                  PlotResetService resetService,
                                  PlotSnapshotService snapshotService,
                                  PlotSignService signService,
                                  WorldEditHook worldEditHook,
                                  TransactionRepository transactionRepository,
                                  PlayerProfileRepository profileRepository,
                                  DistrictManager districtManager,
                                  PropertyTypeManager typeManager,
                                  CityBudgetService budgetService,
                                  DataMigrationService migrationService,
                                  AuctionService auctionService,
                                  RentalService rentalService) {
        this.messageConfig = messageConfig;
        this.plotManagerGuard = id -> plotManager.isResetPending(id) || plotManager.financiallyBlocked(id, null);

        register(new AdminCreateSubCommand(plotManager, worldEditHook, snapshotService, config, messageConfig));
        register(new AdminDeleteSubCommand(plotManager, signService, messageConfig));
        register(new AdminSelectSubCommand(plotManager, messageConfig));
        register(new AdminInfoSubCommand(plotManager, config, messageConfig, taxCalculator));
        register(new AdminSetPriceSubCommand(plotManager, signService, config, messageConfig));
        register(new AdminSetTaxSubCommand(plotManager, signService, config, messageConfig));
        register(new AdminSetTaxModeSubCommand(plotManager, signService, messageConfig));
        register(new AdminSetPeriodSubCommand(plotManager, messageConfig));
        register(new AdminSetGraceSubCommand(plotManager, messageConfig));
        register(new AdminSetOwnerSubCommand(plotManager, signService, profileRepository, messageConfig));
        register(new AdminResetSubCommand(plotManager, resetService, messageConfig));
        register(new AdminSnapshotSubCommand(plotManager, snapshotService, messageConfig));
        register(new AdminRestoreSubCommand(plotManager, resetService, messageConfig));
        register(new AdminSignSubCommand(plotManager, signService, messageConfig));
        register(new AdminUnsignSubCommand(signService, messageConfig));
        register(new AdminListSubCommand(plotManager));
        register(new AdminReloadSubCommand(plugin, messageConfig));
        var auditCommand = new AdminAuditSubCommand(plotManager, transactionRepository, config);
        auditCommand.setAuditService(plugin.getAuditService());
        register(auditCommand);
        register(new AdminDisableSubCommand(plotManager, signService, messageConfig));
        register(new AdminEnableSubCommand(plotManager, signService, messageConfig));
        register(new AdminDistrictSubCommand(districtManager, plotManager));
        register(new AdminTypeSubCommand(typeManager, plotManager));
        register(new AdminBudgetSubCommand(budgetService));
        register(new AdminMigrateSubCommand(migrationService, plugin));
        register(new AdminAuctionSubCommand(auctionService, plotManager));
        register(new AdminRentSubCommand(rentalService, plotManager));
        register(new AdminTransactionsSubCommand(plugin.getOperationJournal(), plugin.getRefundService()));
        register(new AdminHealthSubCommand(plugin, plugin.getStorageBackend(), plugin.getWorldGuardHook(), snapshotService));
    }

    private void register(SubCommand sub) {
        subCommands.put(sub.getName().toLowerCase(), sub);
    }

    public boolean dispatch(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PermissionConfiguration.ADMIN_BASE)) {
            sender.sendMessage(messageConfig.get("general.no-permission"));
            return true;
        }

        if (args.length == 0 || "help".equalsIgnoreCase(args[0])) {
            sendAdminHelp(sender);
            return true;
        }

        String subName = args[0].toLowerCase();
        SubCommand sub = subCommands.get(subName);
        if (sub == null) {
            sender.sendMessage(messageConfig.get("general.unknown-subcommand"));
            return true;
        }

        if (sub.getPermission() != null && !sender.hasPermission(sub.getPermission())) {
            sender.sendMessage(messageConfig.get("general.no-permission"));
            return true;
        }

        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        boolean readOnly = Set.of("help", "info", "list", "audit", "health", "transactions").contains(subName)
                || Set.of("type", "district").contains(subName) && subArgs.length > 0 && Set.of("list", "info").contains(subArgs[0].toLowerCase(Locale.ROOT));
        if (!readOnly && subArgs.length > 0) {
            String id = Set.of("type", "district", "rent", "auction").contains(subName) && subArgs.length > 1 ? subArgs[1] : subArgs[0];
            if (plotManagerGuard.test(id)) {
                sender.sendMessage("Działka ma trwający reset lub operację wymagającą uzgodnienia. Sprawdź /plots admin health i transactions.");
                return true;
            }
        }
        return sub.execute(sender, subArgs);
    }

    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PermissionConfiguration.ADMIN_BASE)) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return subCommands.keySet().stream()
                    .filter(name -> name.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        if (args.length > 1) {
            SubCommand sub = subCommands.get(args[0].toLowerCase());
            if (sub != null) {
                String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
                return sub.tabComplete(sender, subArgs);
            }
        }

        return Collections.emptyList();
    }

    private void sendAdminHelp(CommandSender sender) {
        AdventureUtil.sendMessage(sender, "<gradient:#ff9900:#ff5500><b>=== PANEL ADMINISTRACYJNY RIFTPLOTS ===</b></gradient>");
        for (SubCommand sub : subCommands.values()) {
            AdventureUtil.sendMessage(sender, "<yellow>/" + sub.getUsage() + "</yellow> <gray>- " + sub.getDescription() + "</gray>");
        }
    }
}
