package pl.riftplots.gui;

import org.bukkit.entity.Player;
import pl.riftplots.config.PluginConfiguration;
import pl.riftplots.service.payment.TaxCalculator;
import pl.riftplots.service.plot.PlotManager;

public class PlotListGUI {
    private final MarketMenu market;

    public PlotListGUI(PluginConfiguration config, PlotManager plots, TaxCalculator taxes) {
        market = new MarketMenu(config, plots, taxes);
    }

    public MarketMenu market() {
        return market;
    }

    public void open(Player player) {
        market.open(player, MarketView.start(MarketView.Tab.valueOf(pl.riftplots.config.Presentation.gui().value("market.default-tab"))));
    }

    public void openAvailable(Player player) {
        market.open(player, MarketView.start(MarketView.Tab.SALE));
    }
}
