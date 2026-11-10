package pl.riftplots.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.riftplots.config.PluginConfiguration;
import pl.riftplots.domain.plot.Plot;
import pl.riftplots.util.AdventureUtil;

import java.util.List;

public class PlotConfirmBuyGUI {
    private final PluginConfiguration config;
    private java.util.function.Function<Plot, java.math.BigDecimal> price = Plot::getPrice;
    private java.util.function.Function<Plot, java.math.BigDecimal> tax = Plot::getBaseTax;

    public void setPriceResolver(java.util.function.Function<Plot, java.math.BigDecimal> price) {
        this.price = price;
    }

    public void setTaxResolver(java.util.function.Function<Plot, java.math.BigDecimal> tax) {
        this.tax = tax;
    }

    public PlotConfirmBuyGUI(PluginConfiguration config) {
        this.config = config;
    }

    public void open(Player player, Plot plot) {
        PlotGUIHolder holder = new PlotGUIHolder("PLOT_CONFIRM_BUY", plot.getId());
        Inventory inv = Bukkit.createInventory(holder, 27, AdventureUtil.parse("<dark_green><b>Potwierdzenie Zakupu</b></dark_green>"));
        holder.setInventory(inv);

        ItemStack confirmItem = new ItemStack(Material.LIME_WOOL);
        ItemMeta confMeta = confirmItem.getItemMeta();
        confMeta.displayName(AdventureUtil.parse("<green><b>POTWIERDŹ ZAKUP</b></green>"));
        confMeta.lore(List.of(
                AdventureUtil.parse("<gray>Działka: </gray><gold>" + plot.getName() + " (" + plot.getId() + ")</gold>"),
                AdventureUtil.parse("<gray>Cena: </gray><green>" + config.formatMoney(price.apply(plot)) + "</green>"),
                AdventureUtil.parse("<gray>Podatek: </gray><gold>" + config.formatMoney(tax.apply(plot)) + " co " + plot.getBillingPeriodDays() + " dni</gold>"),
                AdventureUtil.parse("<gray>Dodatkowa prowizja kupującego: </gray><green>0</green>"),
                Component.empty(),
                AdventureUtil.parse("<yellow>Kliknij, aby sfinalizować zakup!</yellow>")
        ));
        confirmItem.setItemMeta(confMeta);
        GuiData.set(confirmItem, "quote", price.apply(plot).toPlainString());
        inv.setItem(11, confirmItem);

        ItemStack cancelItem = new ItemStack(Material.RED_WOOL);
        ItemMeta cancelMeta = cancelItem.getItemMeta();
        cancelMeta.displayName(AdventureUtil.parse("<red><b>ANULUJ</b></red>"));
        cancelMeta.lore(List.of(
                AdventureUtil.parse("<gray>Kliknij, aby zrezygnować z zakupu.</gray>")
        ));
        cancelItem.setItemMeta(cancelMeta);
        inv.setItem(15, cancelItem);
        inv.setItem(22, GuiData.item(Material.GLOWSTONE_DUST, "Pokaż granice 3D", "Zamknij podgląd i obejrzyj obrys nieruchomości w świecie."));

        player.openInventory(inv);
    }
}
