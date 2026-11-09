package pl.riftplots.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.riftplots.command.admin.AdminCommandDispatcher;
import pl.riftplots.command.sub.*;
import pl.riftplots.config.MessageConfiguration;
import pl.riftplots.config.PermissionConfiguration;
import pl.riftplots.config.PluginConfiguration;
import pl.riftplots.gui.PlotListGUI;
import pl.riftplots.service.auction.AuctionService;
import pl.riftplots.service.payment.PaymentService;
import pl.riftplots.service.payment.TaxCalculator;
import pl.riftplots.service.plot.PlotManager;
import pl.riftplots.service.rental.RentalService;
import pl.riftplots.service.teleport.TeleportService;
import pl.riftplots.service.transfer.PlotTransferService;
import pl.riftplots.storage.repository.TransactionRepository;

import java.util.*;

public class PlotCommand implements CommandExecutor {
    private final Map<String, SubCommand> subCommands = new HashMap<>();
    private final AdminCommandDispatcher adminDispatcher;
    private final MessageConfiguration messageConfig;
    private final PlotListGUI listGUI;

    public PlotCommand(PluginConfiguration config,
                       MessageConfiguration messageConfig,
                       PlotManager plotManager,
                       TaxCalculator taxCalculator,
                       PaymentService paymentService,
                       TransactionRepository transactionRepository,
                       RentalService rentalService,
                       PlotTransferService transferService,
                       AuctionService auctionService,
                       TeleportService teleportService,
                       PlotListGUI listGUI,
                       AdminCommandDispatcher adminDispatcher) {
        this.messageConfig = messageConfig;
        this.adminDispatcher = adminDispatcher;
        this.listGUI = listGUI;

        register(new HelpSubCommand());
        register(new ListSubCommand(plotManager, config, taxCalculator, listGUI));
        register(new InfoSubCommand(plotManager, config, messageConfig, taxCalculator));
        register(new BuySubCommand(plotManager, paymentService, messageConfig));
        register(new PaySubCommand(plotManager, paymentService, messageConfig));
        register(new ExtendSubCommand(plotManager, paymentService, messageConfig));
        register(new HistorySubCommand(plotManager, transactionRepository, config));
        register(new TrustSubCommand(plotManager, messageConfig));
        register(new UntrustSubCommand(plotManager, messageConfig));
        register(new MembersSubCommand(plotManager, messageConfig));
        register(new AbandonSubCommand(plotManager, paymentService, config, messageConfig));
        register(new GuiSubCommand(listGUI, messageConfig));
        register(new RentSubCommand(plotManager, rentalService, messageConfig));
        register(new TransferSubCommand(plotManager, transferService, messageConfig));
        register(new AcceptSubCommand(transferService, messageConfig));
        register(new DenySubCommand(transferService));
        register(new AuctionSubCommand(auctionService));
        register(new HomeSubCommand(plotManager, teleportService));
        register(new SetHomeSubCommand(plotManager, teleportService));
    }

    private void register(SubCommand sub) {
        subCommands.put(sub.getName().toLowerCase(), sub);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PermissionConfiguration.USER_BASE)) {
            sender.sendMessage(messageConfig.get("general.no-permission"));
            return true;
        }

        if (args.length == 0) {
            if (sender instanceof Player player) {
                if (player.hasPermission(PermissionConfiguration.USER_GUI)) listGUI.open(player);
                else player.sendMessage(messageConfig.get("general.no-permission"));
            } else {
                SubCommand help = subCommands.get("help");
                if (help != null) {
                    help.execute(sender, new String[0]);
                }
            }
            return true;
        }

        String subName = args[0].toLowerCase();
        if ("admin".equals(subName)) {
            String[] adminArgs = Arrays.copyOfRange(args, 1, args.length);
            return adminDispatcher.dispatch(sender, adminArgs);
        }

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
        return sub.execute(sender, subArgs);
    }

    public Map<String, SubCommand> getSubCommands() {
        return Collections.unmodifiableMap(subCommands);
    }

    public AdminCommandDispatcher getAdminDispatcher() {
        return adminDispatcher;
    }
}
