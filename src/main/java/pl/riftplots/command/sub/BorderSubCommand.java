package pl.riftplots.command.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.riftplots.command.SubCommand;
import pl.riftplots.service.plot.PlotManager;
import pl.riftplots.service.visualization.BorderService;

import java.util.List;

public final class BorderSubCommand implements SubCommand {
    private final PlotManager plots;
    private final BorderService borders;

    public BorderSubCommand(PlotManager plots, BorderService borders) {
        this.plots = plots;
        this.borders = borders;
    }

    public String getName() {
        return "border";
    }

    public String getPermission() {
        return "riftplots.border";
    }

    public String getUsage() {
        return pl.riftplots.config.Texts.raw("text.border.usage");
    }

    public String getDescription() {
        return pl.riftplots.config.Texts.raw("text.border.description");
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(pl.riftplots.config.Texts.component("text.border.komenda-tylko-dla-gracza"));
            return true;
        }
        var location = player.getLocation();
        var plot = args.length > 0 ? plots.getPlot(args[0]) : plots.getPlotAt(player.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
        if (plot.isEmpty())
            sender.sendMessage(pl.riftplots.config.Texts.component("text.border.nie-znaleziono-dziaki"));
        else if (!borders.show(player, plot.get()))
            sender.sendMessage(pl.riftplots.config.Texts.component("text.border.podglad-wymaga-tego-samego-swiata-odczekaj-chwile"));
        return true;
    }

    public List<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 1 ? plots.getAllPlots().stream().map(pl.riftplots.domain.plot.Plot::getId).filter(id -> id.startsWith(args[0])).toList() : List.of();
    }
}
