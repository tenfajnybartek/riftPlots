package pl.riftplots.gui;

import org.bukkit.entity.Player;
import pl.riftplots.config.*;
import pl.riftplots.domain.plot.Plot;

import java.math.BigDecimal;
import java.util.function.Function;

public class PlotConfirmBuyGUI {
    private final PluginConfiguration config;
    private Function<Plot, BigDecimal> price = Plot::getPrice, tax = Plot::getBaseTax;

    public PlotConfirmBuyGUI(PluginConfiguration config) {
        this.config = config;
    }

    public void setPriceResolver(Function<Plot, BigDecimal> resolver) {
        price = resolver;
    }

    public void setTaxResolver(Function<Plot, BigDecimal> resolver) {
        tax = resolver;
    }

    public void open(Player player, Plot plot) {
        var gui = Presentation.gui();
        var holder = new PlotGUIHolder("PLOT_CONFIRM_BUY", plot.getId());
        var variables = GuiVariables.plot(plot, config);
        variables.put("price", config.formatMoney(price.apply(plot)));
        variables.put("tax", config.formatMoney(tax.apply(plot)));
        variables.put("buyer_fee", config.formatMoney(BigDecimal.ZERO));
        var inventory = gui.create("buy", holder, variables);
        gui.buttons(inventory, "buy", variables, java.util.Set.of());
        var confirm = gui.enabled("buy", "confirm") ? inventory.getItem(guiSlot("buy", "confirm")) : null;
        if (confirm != null && "confirm".equals(GuiData.get(confirm, "action")))
            GuiData.set(confirm, "quote", price.apply(plot).toPlainString());
        player.openInventory(inventory);
    }

    private int guiSlot(String menu, String action) {
        return Presentation.gui().number(menu + ".buttons." + action + ".slot");
    }
}
