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
import pl.riftplots.service.payment.PaymentService;
import pl.riftplots.service.payment.TaxCalculator;
import pl.riftplots.util.AdventureUtil;
import pl.riftplots.util.TimeUtil;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class PlotDetailsGUI {
    private final PluginConfiguration config;
    private final TaxCalculator taxCalculator;
    private final PaymentService paymentService;

    public PlotDetailsGUI(PluginConfiguration config, TaxCalculator taxCalculator, PaymentService paymentService) {
        this.config = config;
        this.taxCalculator = taxCalculator;
        this.paymentService = paymentService;
    }

    public void open(Player player, Plot plot) {
        PlotGUIHolder holder = new PlotGUIHolder("PLOT_DETAILS", plot.getId());
        Inventory inv = Bukkit.createInventory(holder, 45, AdventureUtil.parse("<gradient:#00c6ff:#0072ff><b>Zarządzanie: " + plot.getName() + "</b></gradient>"));
        holder.setInventory(inv);

        ItemStack infoItem = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = infoItem.getItemMeta();
        infoMeta.displayName(AdventureUtil.parse("<gold><b>Szczegóły Nieruchomości</b></gold>"));
        List<Component> infoLore = new ArrayList<>();
        infoLore.add(AdventureUtil.parse("<gray>ID: </gray><white>" + plot.getId() + "</white>"));
        infoLore.add(AdventureUtil.parse("<gray>Status: </gray><yellow>" + plot.getStatus() + "</yellow>"));
        infoLore.add(AdventureUtil.parse("<gray>Świat: </gray><white>" + plot.getBounds().getWorldName() + "</white>"));
        infoLore.add(AdventureUtil.parse("<gray>Wymiary: </gray><white>" + plot.getBounds().getWidthX() + "x" + plot.getBounds().getLengthZ() + " (" + plot.getBounds().getHorizontalArea() + " m²)</white>"));
        infoLore.add(AdventureUtil.parse("<gray>Zakupiono: </gray><white>" + TimeUtil.formatInstant(plot.getPurchaseDate()) + "</white>"));
        infoLore.add(AdventureUtil.parse("<gray>Następny podatek: </gray><yellow>" + TimeUtil.formatInstant(plot.getNextTaxPayment()) + "</yellow>"));
        infoMeta.lore(infoLore);
        infoItem.setItemMeta(infoMeta);
        inv.setItem(11, infoItem);

        ItemStack payItem = new ItemStack(Material.EMERALD);
        ItemMeta payMeta = payItem.getItemMeta();
        payMeta.displayName(AdventureUtil.parse("<green><b>Opłać Podatek</b></green>"));
        List<Component> payLore = new ArrayList<>();
        BigDecimal tax = taxCalculator.calculateTax(plot);
        payLore.add(AdventureUtil.parse("<gray>Stawka podatku: </gray><gold>" + config.formatMoney(tax) + "</gold>"));
        payLore.add(AdventureUtil.parse("<gray>Okres: </gray><white>" + plot.getBillingPeriodDays() + " dni</white>"));
        payLore.add(AdventureUtil.parse("<gray>Opłacono z góry: </gray><aqua>" + plot.getAdvancePeriodsPaid() + "/" + config.getMaxAdvancePeriods() + "</aqua>"));
        payLore.add(Component.empty());
        payLore.add(AdventureUtil.parse("<yellow>Kliknij, aby opłacić 1 okres podatkowy!</yellow>"));
        payMeta.lore(payLore);
        payItem.setItemMeta(payMeta);
        inv.setItem(13, payItem);

        ItemStack membersItem = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta membersMeta = membersItem.getItemMeta();
        membersMeta.displayName(AdventureUtil.parse("<aqua><b>Współużytkownicy</b></aqua>"));
        List<Component> memLore = new ArrayList<>();
        memLore.add(AdventureUtil.parse("<gray>Liczba członków: </gray><yellow>" + plot.getMembers().size() + "</yellow>"));
        memLore.add(Component.empty());
        memLore.add(AdventureUtil.parse("<yellow>Kliknij, aby zarządzać członkami!</yellow>"));
        membersMeta.lore(memLore);
        membersItem.setItemMeta(membersMeta);
        inv.setItem(15, membersItem);

        ItemStack abandonItem = new ItemStack(Material.BARRIER);
        ItemMeta abMeta = abandonItem.getItemMeta();
        abMeta.displayName(AdventureUtil.parse("<red><b>Porzuć Nieruchomość</b></red>"));
        List<Component> abLore = new ArrayList<>();
        BigDecimal refund = paymentService.calculateAbandonRefund(plot);
        abLore.add(AdventureUtil.parse("<gray>Zwrot środków: </gray><green>" + config.formatMoney(refund) + "</green>"));
        abLore.add(AdventureUtil.parse("<red>Utracisz prawo własności do tej działki!</red>"));
        abLore.add(Component.empty());
        abLore.add(AdventureUtil.parse("<red>Kliknij, aby oddać działkę miastu.</red>"));
        abMeta.lore(abLore);
        abandonItem.setItemMeta(abMeta);
        inv.setItem(31, abandonItem);

        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.displayName(AdventureUtil.parse("<yellow><b>Powrót</b></yellow>"));
        backItem.setItemMeta(backMeta);
        inv.setItem(36, backItem);

        player.openInventory(inv);
    }
}
