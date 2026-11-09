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
import pl.riftplots.service.payment.TaxCalculator;
import pl.riftplots.service.plot.PlotManager;
import pl.riftplots.util.AdventureUtil;

import java.util.ArrayList;
import java.util.List;

public class PlotListGUI {
    private final PluginConfiguration config;
    private final PlotManager plotManager;
    private final TaxCalculator taxCalculator;

    public PlotListGUI(PluginConfiguration config, PlotManager plotManager, TaxCalculator taxCalculator) {
        this.config = config;
        this.plotManager = plotManager;
        this.taxCalculator = taxCalculator;
    }

    public void open(Player player) {
        PlotGUIHolder holder = new PlotGUIHolder("PLOT_LIST", null);
        Inventory inv = Bukkit.createInventory(holder, 54, AdventureUtil.parse("<gradient:#00c6ff:#0072ff><b>Twoje Działki</b></gradient>"));
        holder.setInventory(inv);

        List<Plot> ownedPlots = plotManager.getPlotsByOwner(player.getUniqueId());
        int slot = 0;
        for (Plot plot : ownedPlots) {
            if (slot >= 45) {
                break;
            }
            inv.setItem(slot++, createPlotItem(plot));
        }

        ItemStack availableButton = new ItemStack(Material.EMERALD);
        ItemMeta avMeta = availableButton.getItemMeta();
        avMeta.displayName(AdventureUtil.parse("<green><b>Dostępne Działki w Mieście</b></green>"));
        List<Component> avLore = List.of(
                AdventureUtil.parse("<gray>Kliknij, aby przeglądać wolne</gray>"),
                AdventureUtil.parse("<gray>nieruchomości gotowe do zakupu.</gray>")
        );
        avMeta.lore(avLore);
        availableButton.setItemMeta(avMeta);
        inv.setItem(49, availableButton);

        player.openInventory(inv);
    }

    public void openAvailable(Player player) {
        PlotGUIHolder holder = new PlotGUIHolder("PLOT_AVAILABLE_LIST", null);
        Inventory inv = Bukkit.createInventory(holder, 54, AdventureUtil.parse("<gradient:#00c6ff:#0072ff><b>Działki na Sprzedaż</b></gradient>"));
        holder.setInventory(inv);

        int slot = 0;
        for (Plot plot : plotManager.getAllPlots()) {
            if (slot >= 45) {
                break;
            }
            if (plot.isAvailableForPurchase()) {
                inv.setItem(slot++, createAvailablePlotItem(plot));
            }
        }

        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.displayName(AdventureUtil.parse("<yellow><b>Powrót</b></yellow>"));
        backButton.setItemMeta(backMeta);
        inv.setItem(49, backButton);

        player.openInventory(inv);
    }

    private ItemStack createPlotItem(Plot plot) {
        ItemStack item = new ItemStack(Material.GOLD_BLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(AdventureUtil.parse("<gradient:#00c6ff:#0072ff><b>" + plot.getName() + "</b></gradient> <dark_gray>(" + plot.getId() + ")</dark_gray>"));

        List<Component> lore = new ArrayList<>();
        lore.add(AdventureUtil.parse("<gray>Status: </gray><yellow>" + plot.getStatus() + "</yellow>"));
        lore.add(AdventureUtil.parse("<gray>Powierzchnia: </gray><white>" + plot.getBounds().getHorizontalArea() + " m²</white>"));
        lore.add(AdventureUtil.parse("<gray>Podatek: </gray><gold>" + config.formatMoney(taxCalculator.calculateTax(plot)) + "</gold>"));
        lore.add(AdventureUtil.parse("<gray>Współużytkownicy: </gray><aqua>" + plot.getMembers().size() + "</aqua>"));
        lore.add(Component.empty());
        lore.add(AdventureUtil.parse("<yellow>Kliknij, aby zarządzać tą działką!</yellow>"));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createAvailablePlotItem(Plot plot) {
        ItemStack item = new ItemStack(Material.GRASS_BLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(AdventureUtil.parse("<green><b>" + plot.getName() + "</b></green> <dark_gray>(" + plot.getId() + ")</dark_gray>"));

        List<Component> lore = new ArrayList<>();
        lore.add(AdventureUtil.parse("<gray>Cena: </gray><green>" + config.formatMoney(plot.getPrice()) + "</green>"));
        lore.add(AdventureUtil.parse("<gray>Podatek: </gray><gold>" + config.formatMoney(taxCalculator.calculateTax(plot)) + "</gold>"));
        lore.add(AdventureUtil.parse("<gray>Powierzchnia: </gray><white>" + plot.getBounds().getHorizontalArea() + " m²</white>"));
        lore.add(Component.empty());
        lore.add(AdventureUtil.parse("<yellow>Kliknij, aby przejść do zakupu!</yellow>"));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
