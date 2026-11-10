package pl.riftplots.command.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
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
import java.util.Optional;

public class HistorySubCommand implements SubCommand {
    private final PlotManager plotManager;
    private final TransactionRepository transactionRepository;
    private final PluginConfiguration config;
    private pl.riftplots.service.transaction.OperationJournal journal;
    private pl.riftplots.service.transaction.RefundService refunds;

    public void setLedger(pl.riftplots.service.transaction.OperationJournal journal, pl.riftplots.service.transaction.RefundService refunds) {
        this.journal = journal;
        this.refunds = refunds;
    }

    public HistorySubCommand(PlotManager plotManager, TransactionRepository transactionRepository, PluginConfiguration config) {
        this.plotManager = plotManager;
        this.transactionRepository = transactionRepository;
        this.config = config;
    }

    @Override
    public String getName() {
        return "history";
    }

    @Override
    public String getPermission() {
        return PermissionConfiguration.USER_HISTORY;
    }

    @Override
    public String getUsage() {
        return pl.riftplots.config.Texts.raw("text.history.usage");
    }

    @Override
    public String getDescription() {
        return pl.riftplots.config.Texts.raw("text.history.description");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        String plotId = null;
        int page = 0;
        try {
            if (args.length > 3 || args.length == 3 && !args[2].equalsIgnoreCase("legacy"))
                throw new IllegalArgumentException(pl.riftplots.config.Texts.raw("text.history.uzycie", getUsage()));
            if (args.length >= 2) page = pl.riftplots.command.BillingArguments.positiveInt(args[1]) - 1;
        } catch (IllegalArgumentException e) {
            sender.sendMessage(net.kyori.adventure.text.Component.text(e.getMessage()));
            return true;
        }
        if (args.length > 0 && !args[0].equalsIgnoreCase("all")) {
            Plot plot = plotManager.getPlot(args[0]).orElse(null);
            if (plot == null) {
                sender.sendMessage(pl.riftplots.config.Texts.component("text.history.dziaka-nie-istnieje"));
                return true;
            }
            plotId = plot.getId();
        } else if (args.length == 0 && sender instanceof Player player) {
            Optional<Plot> opt = plotManager.getPlotAt(
                    player.getWorld().getName(),
                    player.getLocation().getBlockX(),
                    player.getLocation().getBlockY(),
                    player.getLocation().getBlockZ()
            );
            if (opt.isPresent() && (config.isPublicHistory() || opt.get().isOwner(player.getUniqueId()) || player.hasPermission("riftplots.admin.history"))) {
                plotId = opt.get().getId();
            }
        }
        if (sender instanceof Player player && plotId != null && !config.isPublicHistory()
                && !plotManager.getPlot(plotId).map(plot -> plot.isOwner(player.getUniqueId())).orElse(false)
                && !player.hasPermission("riftplots.admin.history")) {
            sender.sendMessage(pl.riftplots.config.Texts.component("text.history.historia-tej-nieruchomosci-jest-prywatna"));
            return true;
        }
        if (sender instanceof Player player && refunds != null)
            refunds.list(player.getUniqueId()).stream().limit(pl.riftplots.config.Settings.integer("paging.refunds-in-player-history"))
                    .forEach(refund -> sender.sendMessage(pl.riftplots.config.Texts.component("text.history.zwrot", refund.id(), config.formatMoney(refund.amount()), refund.status())));
        if (journal != null && args.length < 3) {
            java.util.UUID playerId = sender instanceof Player player && plotId == null ? player.getUniqueId() : null;
            var entries = journal.history(playerId, plotId, page, pl.riftplots.config.Settings.integer("paging.history"));
            if (!entries.isEmpty() || page > 0) {
                sender.sendMessage(pl.riftplots.config.Texts.component("text.history.historia-operacji-strona", (page + 1)));
                entries.forEach(entry -> sender.sendMessage(pl.riftplots.config.Texts.component("text.history.line", entry.id(), entry.created(), entry.kind(), config.formatMoney(entry.amount()), entry.status())));
                sender.sendMessage(pl.riftplots.config.Texts.component("text.history.starsze-zapisy-plots-history-legacy", (plotId == null ? "all" : plotId), (page + 1)));
                return true;
            }
        }

        List<TransactionRecord> list;
        if (plotId != null) {
            list = transactionRepository.findByPlotId(plotId, page, pl.riftplots.config.Settings.integer("paging.history"));
            AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.history.historia-transakcji-dziaki", plotId.toUpperCase()));
        } else if (sender instanceof Player player) {
            list = transactionRepository.findByPlayer(player.getUniqueId(), page, pl.riftplots.config.Settings.integer("paging.history"));
            AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.history.twoja-historia-transakcji"));
        } else {
            list = transactionRepository.findAll(page, pl.riftplots.config.Settings.integer("paging.history"));
            AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.history.ostatnie-transakcje"));
        }

        if (list.isEmpty()) {
            AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.history.brak-zarejestrowanych-transakcji"));
            return true;
        }

        for (TransactionRecord record : list) {
            AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.history.line-252d238", TimeUtil.formatInstant(record.getTimestamp()), record.getType(), config.formatMoney(record.getAmount()), record.getDetails()));
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return plotManager.getAllPlots().stream()
                    .filter(plot -> config.isPublicHistory() || !(sender instanceof Player player) || plot.isOwner(player.getUniqueId()) || player.hasPermission("riftplots.admin.history"))
                    .map(Plot::getId)
                    .filter(id -> id.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return Collections.emptyList();
    }
}
