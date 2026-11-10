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
        return "plots history [id|all] [strona] [legacy]";
    }

    @Override
    public String getDescription() {
        return "Wyświetla historię płatności i transakcji.";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        String plotId = null;
        int page = 0;
        try {
            if (args.length > 3 || args.length == 3 && !args[2].equalsIgnoreCase("legacy"))
                throw new IllegalArgumentException("Użycie: /" + getUsage());
            if (args.length >= 2) page = pl.riftplots.command.BillingArguments.positiveInt(args[1]) - 1;
        } catch (IllegalArgumentException e) {
            sender.sendMessage(net.kyori.adventure.text.Component.text(e.getMessage()));
            return true;
        }
        if (args.length > 0 && !args[0].equalsIgnoreCase("all")) {
            Plot plot = plotManager.getPlot(args[0]).orElse(null);
            if (plot == null) {
                sender.sendMessage("Działka nie istnieje.");
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
            sender.sendMessage("Historia tej nieruchomości jest prywatna.");
            return true;
        }
        if (sender instanceof Player player && refunds != null) refunds.list(player.getUniqueId()).stream().limit(10)
                .forEach(refund -> sender.sendMessage(net.kyori.adventure.text.Component.text("Zwrot " + refund.id() + " | " + config.formatMoney(refund.amount()) + " | " + refund.status())));
        if (journal != null && args.length < 3) {
            java.util.UUID playerId = sender instanceof Player player && plotId == null ? player.getUniqueId() : null;
            var entries = journal.history(playerId, plotId, page, 10);
            if (!entries.isEmpty() || page > 0) {
                sender.sendMessage("Historia operacji — strona " + (page + 1));
                entries.forEach(entry -> sender.sendMessage(net.kyori.adventure.text.Component.text(
                        entry.id() + " | " + entry.created() + " | " + entry.kind() + " | " + config.formatMoney(entry.amount()) + " | " + entry.status())));
                sender.sendMessage("Starsze zapisy: /plots history " + (plotId == null ? "all" : plotId) + " " + (page + 1) + " legacy");
                return true;
            }
        }

        List<TransactionRecord> list;
        if (plotId != null) {
            list = transactionRepository.findByPlotId(plotId, page, 10);
            AdventureUtil.sendMessage(sender, "<gradient:#00c6ff:#0072ff><b>=== HISTORIA TRANSAKCJI DZIAŁKI " + plotId.toUpperCase() + " ===</b></gradient>");
        } else if (sender instanceof Player player) {
            list = transactionRepository.findByPlayer(player.getUniqueId(), page, 10);
            AdventureUtil.sendMessage(sender, "<gradient:#00c6ff:#0072ff><b>=== TWOJA HISTORIA TRANSAKCJI ===</b></gradient>");
        } else {
            list = transactionRepository.findAll(page, 10);
            AdventureUtil.sendMessage(sender, "<gradient:#00c6ff:#0072ff><b>=== OSTATNIE TRANSAKCJE ===</b></gradient>");
        }

        if (list.isEmpty()) {
            AdventureUtil.sendMessage(sender, "<gray>Brak zarejestrowanych transakcji.</gray>");
            return true;
        }

        for (TransactionRecord record : list) {
            AdventureUtil.sendMessage(sender, "<gray>[" + TimeUtil.formatInstant(record.getTimestamp()) + "]</gray> <gold>" + record.getType() + "</gold> <yellow>" + config.formatMoney(record.getAmount()) + "</yellow> <white>(" + record.getDetails() + ")</white>");
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
