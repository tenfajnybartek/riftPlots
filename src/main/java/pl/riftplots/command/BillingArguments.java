package pl.riftplots.command;

import org.bukkit.entity.Player;
import pl.riftplots.domain.plot.Plot;
import pl.riftplots.service.plot.PlotManager;

public record BillingArguments(Plot plot, int periods) {
    public static int positiveInt(String value) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 1) throw new NumberFormatException();
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(pl.riftplots.config.Texts.raw("text.billing-arguments.liczba-okresow-musi-byc-dodatnia-liczba-cakowita"));
        }
    }

    public static BillingArguments resolve(Player player, String[] args, PlotManager plots) {
        if (args.length > 2)
            throw new IllegalArgumentException(pl.riftplots.config.Texts.raw("text.billing-arguments.zbyt-wiele-argumentow-podaj-dziaka-okresy"));
        Plot plot = null;
        int periods = pl.riftplots.config.Settings.integer("billing.default-payment-periods");
        if (args.length > 0) {
            plot = plots.getPlot(args[0]).orElse(null);
            if (plot != null) {
                if (args.length == 2) periods = positiveInt(args[1]);
            } else if (args.length == 1 && args[0].matches("[+-]?[0-9]+")) periods = positiveInt(args[0]);
            else
                throw new IllegalArgumentException(pl.riftplots.config.Texts.raw("text.billing-arguments.dziaka-nie-istnieje-podaj-poprawne-id"));
        }
        if (plot == null) {
            var location = player.getLocation();
            plot = plots.getPlotAt(player.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ()).orElse(null);
        }
        if (plot == null)
            throw new IllegalArgumentException(pl.riftplots.config.Texts.raw("text.billing-arguments.podaj-identyfikator-dziaki-lub-stan-na-jej"));
        return new BillingArguments(plot, periods);
    }
}
