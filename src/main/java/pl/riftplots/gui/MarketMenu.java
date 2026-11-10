package pl.riftplots.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import pl.riftplots.config.PluginConfiguration;
import pl.riftplots.domain.plot.Plot;
import pl.riftplots.service.plot.PlotManager;
import pl.riftplots.service.payment.TaxCalculator;
import pl.riftplots.service.auction.AuctionService;
import pl.riftplots.util.AdventureUtil;

import java.util.*;

public final class MarketMenu {
    private final PluginConfiguration config;
    private final PlotManager plots;
    private final TaxCalculator taxes;
    private AuctionService auctions;

    public MarketMenu(PluginConfiguration config, PlotManager plots, TaxCalculator taxes) {
        this.config = config;
        this.plots = plots;
        this.taxes = taxes;
    }

    public void setAuctions(AuctionService auctions) {
        this.auctions = auctions;
    }

    private java.math.BigDecimal price(Plot plot) {
        return Optional.ofNullable(plots.getPurchasePrice(plot)).orElse(plot.getPrice());
    }

    public List<Plot> matching(UUID player, MarketView view) {
        var comparator = switch (view.sort()) {
            case PRICE -> Comparator.comparing(this::price);
            case TAX -> Comparator.comparing(taxes::calculateTax);
            case ID -> Comparator.comparing(Plot::getId);
        };
        return plots.getAllPlots().stream().filter(plot -> switch (view.tab()) {
                    case OWNED -> plot.isOwner(player) || plot.isMember(player);
                    case SALE -> plot.isAvailableForPurchase();
                    case RENT ->
                            plot.isRentalAvailable() && !plot.isRented() && !plot.isAuctionActive() && (plot.getStatus() == pl.riftplots.domain.plot.PlotStatus.OWNED || plot.getStatus() == pl.riftplots.domain.plot.PlotStatus.AVAILABLE);
                    case AUCTION -> plot.isAuctionActive();
                }).filter(plot -> view.district() == null || view.district().equals(plot.getDistrictId()))
                .filter(plot -> view.propertyType() == null || view.propertyType().equals(plot.getPropertyType()))
                .sorted(comparator.thenComparing(Plot::getId)).toList();
    }

    public void open(Player player, MarketView requested) {
        List<Plot> all = matching(player.getUniqueId(), requested);
        MarketView view = requested.withPage(Math.min(requested.page(), Math.max(0, (all.size() - 1) / 45)));
        var holder = new PlotGUIHolder("PLOT_MARKET", null);
        holder.setMarketView(view);
        var inventory = Bukkit.createInventory(holder, 54, AdventureUtil.parse("<gradient:#00c6ff:#0072ff><b>Rynek nieruchomości · " + (view.page() + 1) + "</b></gradient>"));
        holder.setInventory(inventory);
        List<Plot> page = all.stream().skip((long) view.page() * 45).limit(45).toList();
        for (int i = 0; i < page.size(); i++) {
            Plot plot = page.get(i);
            var icon = Optional.ofNullable(plots.getIcon(plot)).orElse(Material.GRASS_BLOCK);
            var item = GuiData.item(icon,
                    plot.getName() + " (" + plot.getId() + ")", "Typ: " + plot.getPropertyType() + " | Dzielnica: " + Objects.toString(plot.getDistrictId(), "brak"),
                    "Cena: " + config.formatMoney(price(plot)), "Podatek: " + config.formatMoney(taxes.calculateTax(plot)),
                    "Czynsz: " + config.formatMoney(plot.getRentalPrice()) + " / " + plot.getRentalPeriodUnit(), "Kliknij, aby zobaczyć szczegóły.");
            GuiData.set(item, "plot", plot.getId());
            inventory.setItem(i, item);
        }
        if (view.page() > 0) inventory.setItem(45, GuiData.item(Material.ARROW, "Poprzednia strona"));
        if ((long) (view.page() + 1) * 45 < all.size())
            inventory.setItem(53, GuiData.item(Material.ARROW, "Następna strona"));
        inventory.setItem(46, GuiData.item(Material.GOLD_BLOCK, "Moje"));
        inventory.setItem(47, GuiData.item(Material.EMERALD, "Dostępne"));
        inventory.setItem(48, GuiData.item(Material.OAK_DOOR, "Wynajem"));
        inventory.setItem(49, GuiData.item(Material.GOLDEN_HOE, "Aukcje"));
        inventory.setItem(50, GuiData.item(Material.COMPARATOR, "Sortowanie: " + view.sort()));
        inventory.setItem(51, GuiData.item(Material.MAP, "Dzielnica: " + Objects.toString(view.district(), "wszystkie")));
        inventory.setItem(52, GuiData.item(Material.BRICKS, "Typ: " + Objects.toString(view.propertyType(), "wszystkie")));
        player.openInventory(inventory);
    }

    public void navigate(Player player, MarketView view, int slot) {
        MarketView next = switch (slot) {
            case 45 -> view.withPage(view.page() - 1);
            case 53 -> view.withPage(view.page() + 1);
            case 46, 47, 48, 49 -> MarketView.start(MarketView.Tab.values()[slot - 46]);
            case 50 ->
                    new MarketView(view.tab(), 0, MarketView.Sort.values()[(view.sort().ordinal() + 1) % MarketView.Sort.values().length], view.district(), view.propertyType());
            case 51 ->
                    new MarketView(view.tab(), 0, view.sort(), cycle(view.district(), plots.getAllPlots().stream().map(Plot::getDistrictId).filter(Objects::nonNull).distinct().sorted().toList()), view.propertyType());
            case 52 ->
                    new MarketView(view.tab(), 0, view.sort(), view.district(), cycle(view.propertyType(), plots.getAllPlots().stream().map(Plot::getPropertyType).distinct().sorted().toList()));
            default -> view;
        };
        open(player, next);
    }

    private String cycle(String value, List<String> options) {
        int next = options.indexOf(value) + 1;
        return next < options.size() ? options.get(next) : null;
    }

    public void confirmRental(Player player, Plot plot, boolean autoRenew) {
        var holder = new PlotGUIHolder("PLOT_CONFIRM_RENT", plot.getId());
        holder.setAutoRenew(autoRenew);
        var inventory = Bukkit.createInventory(holder, 27, AdventureUtil.parse("<dark_green>Potwierdź najem</dark_green>"));
        holder.setInventory(inventory);
        var cost = plot.getRentalPrice().add(plot.getRentalDeposit());
        var item = GuiData.item(Material.LIME_WOOL, "Wynajmij " + plot.getName(), "Czynsz: " + config.formatMoney(plot.getRentalPrice()) + " / " + plot.getRentalPeriodUnit(),
                "Kaucja: " + config.formatMoney(plot.getRentalDeposit()), "Do zapłaty: " + config.formatMoney(cost), "Podatek właściciela: " + config.formatMoney(taxes.calculateTax(plot)), "Auto-odnowienie: " + autoRenew);
        GuiData.set(item, "quote", cost.toPlainString());
        GuiData.set(item, "terms", plot.getRentalPrice() + ":" + plot.getRentalDeposit() + ":" + plot.getRentalPeriodUnit());
        inventory.setItem(11, item);
        inventory.setItem(13, GuiData.item(Material.REPEATER, "Auto-odnowienie: " + autoRenew, "Kliknij, aby zmienić przed potwierdzeniem."));
        inventory.setItem(15, GuiData.item(Material.RED_WOOL, "Anuluj"));
        player.openInventory(inventory);
    }

    public void auction(Player player, Plot plot) {
        if (auctions == null) return;
        var auction = auctions.getAuction(plot.getId()).orElse(null);
        if (auction == null) {
            open(player, MarketView.start(MarketView.Tab.AUCTION));
            return;
        }
        var holder = new PlotGUIHolder("PLOT_AUCTION", plot.getId());
        var inventory = Bukkit.createInventory(holder, 27, AdventureUtil.parse("<gold>Aukcja: " + plot.getId() + "</gold>"));
        holder.setInventory(inventory);
        var minimum = auction.getCurrentBidderUuid() == null ? auction.getStartingPrice() : auction.getCurrentBid().add(auction.getMinBidIncrement());
        if (auction.getBuyoutPrice() != null) minimum = minimum.min(auction.getBuyoutPrice());
        var reserved = auction.isEscrowEnabled() && player.getUniqueId().equals(auction.getCurrentBidderUuid())
                ? auction.getCurrentBid() : java.math.BigDecimal.ZERO;
        var bid = GuiData.item(Material.LIME_WOOL, "Licytuj: " + config.formatMoney(minimum), "Prowizja miasta: " + config.getAuctionCityFeePercent() + "% (z ceny)",
                "Podatek: " + config.formatMoney(taxes.calculateTax(plot)) + " co " + plot.getBillingPeriodDays() + " dni", "Koniec: " + auction.getEndTime(),
                auction.isEscrowEnabled() ? "Depozyt teraz: " + config.formatMoney(minimum.subtract(reserved)) : "Pobranie ceny zwycięskiej przy rozliczeniu.",
                "Kliknięcie składa ofertę w podanej kwocie.");
        GuiData.set(bid, "auction", auction.getId());
        GuiData.set(bid, "quote", minimum.toPlainString());
        inventory.setItem(11, bid);
        var history = new ArrayList<>(auction.getBids());
        Collections.reverse(history);
        var lines = history.stream().limit(4).map(value -> value.getBidderName() + ": " + config.formatMoney(value.getAmount())).toArray(String[]::new);
        inventory.setItem(13, GuiData.item(Material.BOOK, "Historia przebić", lines));
        if (auction.getBuyoutPrice() != null) {
            var buy = GuiData.item(Material.EMERALD, "Kup teraz: " + config.formatMoney(auction.getBuyoutPrice()),
                    "Do pobrania teraz: " + config.formatMoney(auction.getBuyoutPrice().subtract(reserved)), "Natychmiastowe rozliczenie aukcji.");
            GuiData.set(buy, "auction", auction.getId());
            GuiData.set(buy, "quote", auction.getBuyoutPrice().toPlainString());
            inventory.setItem(15, buy);
        }
        inventory.setItem(22, GuiData.item(Material.ARROW, "Powrót"));
        player.openInventory(inventory);
    }
}
