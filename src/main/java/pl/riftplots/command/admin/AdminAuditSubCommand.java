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
        return pl.riftplots.config.Texts.raw("text.admin-audit.usage");
    }

    @Override
    public String getDescription() {
        return pl.riftplots.config.Texts.raw("text.admin-audit.description");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 1 || args.length > 2) {
            AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-audit.uzycie", getUsage()));
            return true;
        }

        String id = args[0].toLowerCase();
        int page;
        try {
            page = args.length == 2 ? pl.riftplots.command.BillingArguments.positiveInt(args[1]) - 1 : 0;
        } catch (IllegalArgumentException e) {
            sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-audit.uzycie-c161434", getUsage()));
            return true;
        }
        sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-audit.audyt-strona", (page + 1)));
        if (audit != null)
            for (var entry : audit.getLogsForPlot(id, page, pl.riftplots.config.Settings.integer("paging.audit")))
                sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-audit.line", entry.getTimestamp(), entry.getOperatorName(), entry.getAction(), entry.getDetails()));
        List<TransactionRecord> records = transactionRepository.findByPlotId(id, page, pl.riftplots.config.Settings.integer("paging.audit"));

        AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-audit.audyt-transakcji-dziaki", id.toUpperCase()));
        if (records.isEmpty()) {
            AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-audit.brak-zarejestrowanych-transakcji-dla-tej-dziaki"));
            return true;
        }

        for (TransactionRecord record : records.stream().toList()) {
            AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-audit.line-c252bf8", TimeUtil.formatInstant(record.getTimestamp()), record.getType(), config.formatMoney(record.getAmount()), record.getPlayerName(), record.getStatus(), record.getDetails()));
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
