package pl.riftplots.gui;

import org.bukkit.entity.Player;
import pl.riftplots.config.*;
import pl.riftplots.domain.plot.Plot;
import pl.riftplots.service.payment.*;

public class PlotDetailsGUI {
    private final PluginConfiguration config;
    private final TaxCalculator taxes;
    private final PaymentService payments;

    public PlotDetailsGUI(PluginConfiguration config, TaxCalculator taxes, PaymentService payments) {
        this.config = config;
        this.taxes = taxes;
        this.payments = payments;
    }

    public void open(Player player, Plot plot) {
        var variables = GuiVariables.plot(plot, config);
        variables.put("tax", config.formatMoney(taxes.calculateTax(plot)));
        variables.put("refund", config.formatMoney(payments.calculateAbandonRefund(plot)));
        variables.put("pay_periods", Settings.integer("billing.default-payment-periods"));
        var gui = Presentation.gui();
        var inventory = gui.create("details", new PlotGUIHolder("PLOT_DETAILS", plot.getId()), variables);
        gui.buttons(inventory, "details", variables, java.util.Set.of());
        player.openInventory(inventory);
    }
}
