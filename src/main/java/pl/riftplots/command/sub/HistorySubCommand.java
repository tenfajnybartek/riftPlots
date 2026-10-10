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
        return "plots history [działka]";
    }

    @Override
    public String getDescription() {
        return "Wyświetla historię płatności i transakcji.";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        String plotId = null;
        if (args.length > 0) {
            plotId = args[0];
        } else if (sender instanceof Player player) {
            Optional<Plot> opt = plotManager.getPlotAt(
                    player.getWorld().getName(),
                    player.getLocation().getBlockX(),
                    player.getLocation().getBlockY(),
                    player.getLocation().getBlockZ()
            );
            if (opt.isPresent()) {
                plotId = opt.get().getId();
            }
        }

        List<TransactionRecord> list;
        if (plotId != null) {
            list = transactionRepository.findByPlotId(plotId);
            AdventureUtil.sendMessage(sender, "<gradient:#00c6ff:#0072ff><b>=== HISTORIA TRANSAKCJI DZIAŁKI " + plotId.toUpperCase() + " ===</b></gradient>");
        } else if (sender instanceof Player player) {
            list = transactionRepository.findByPlayer(player.getUniqueId());
            AdventureUtil.sendMessage(sender, "<gradient:#00c6ff:#0072ff><b>=== TWOJA HISTORIA TRANSAKCJI ===</b></gradient>");
        } else {
            list = transactionRepository.findAll(20);
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
                    .map(Plot::getId)
                    .filter(id -> id.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return Collections.emptyList();
    }
}
