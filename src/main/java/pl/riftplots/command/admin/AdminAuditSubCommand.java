package pl.riftplots.command.admin;

import org.bukkit.command.CommandSender;
import pl.riftplots.command.SubCommand;
import pl.riftplots.config.PermissionConfiguration;
import pl.riftplots.config.PluginConfiguration;
import pl.riftplots.domain.payment.TransactionRecord;
import pl.riftplots.domain.plot.Plot;
import pl.riftplots.service.plot.PlotManager;
import pl.riftplots.storage.repository.TransactionRepository;
import pl.riftplots.util.AdventureUtil;
import pl.riftplots.util.TimeUtil;

import java.util.Collections;
import java.util.List;

public class AdminAuditSubCommand implements SubCommand {
    private final PlotManager plotManager;
    private final TransactionRepository transactionRepository;
    private final PluginConfiguration config;
    private pl.riftplots.service.audit.AuditService audit;

    public void setAuditService(pl.riftplots.service.audit.AuditService audit) {
        this.audit = audit;
    }

    public AdminAuditSubCommand(PlotManager plotManager, TransactionRepository transactionRepository, PluginConfiguration config) {
        this.plotManager = plotManager;
        this.transactionRepository = transactionRepository;
        this.config = config;
    }

    @Override
    public String getName() {
        return "audit";
    }

    @Override
    public String getPermission() {
        return PermissionConfiguration.ADMIN_AUDIT;
    }

    @Override
    public String getUsage() {
        return "plots admin audit <id> [strona]";
    }

    @Override
    public String getDescription() {
        return "Przeprowadza audyt finansowy i operacyjny wybranej działki.";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 1 || args.length > 2) {
            AdventureUtil.sendMessage(sender, "<red>Użycie: /" + getUsage() + "</red>");
            return true;
        }

        String id = args[0].toLowerCase();
        int page;
        try {
            page = args.length == 2 ? pl.riftplots.command.BillingArguments.positiveInt(args[1]) - 1 : 0;
        } catch (IllegalArgumentException e) {
            sender.sendMessage("Użycie: /" + getUsage());
            return true;
        }
        sender.sendMessage("Audyt — strona " + (page + 1));
        if (audit != null) for (var entry : audit.getLogsForPlot(id, page, 20))
            sender.sendMessage(net.kyori.adventure.text.Component.text(
                    entry.getTimestamp() + " | " + entry.getOperatorName() + " | " + entry.getAction() + " | " + entry.getDetails()));
        List<TransactionRecord> records = transactionRepository.findByPlotId(id, page, 20);

        AdventureUtil.sendMessage(sender, "<gradient:#ff9900:#ff5500><b>=== AUDYT TRANSAKCJI DZIAŁKI " + id.toUpperCase() + " ===</b></gradient>");
        if (records.isEmpty()) {
            AdventureUtil.sendMessage(sender, "<gray>Brak zarejestrowanych transakcji dla tej działki.</gray>");
            return true;
        }

        for (TransactionRecord record : records.stream().limit(20).toList()) {
            AdventureUtil.sendMessage(sender, "<gray>[" + TimeUtil.formatInstant(record.getTimestamp()) + "]</gray> <gold>" + record.getType() + "</gold> <yellow>" + config.formatMoney(record.getAmount()) + "</yellow> <aqua>" + record.getPlayerName() + "</aqua> <dark_gray>(" + record.getStatus() + " - " + record.getDetails() + ")</dark_gray>");
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return plotManager.getAllPlots().stream()
                    .map(Plot::getId)
                    .filter(id -> id.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return Collections.emptyList();
    }
}
