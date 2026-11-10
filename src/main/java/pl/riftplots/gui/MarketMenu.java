package pl.riftplots.gui;

import org.bukkit.entity.Player;
import pl.riftplots.config.*;
import pl.riftplots.domain.plot.Plot;
import pl.riftplots.service.plot.PlotManager;
import pl.riftplots.service.payment.TaxCalculator;
import pl.riftplots.service.auction.AuctionService;

import java.util.*;
import java.math.BigDecimal;

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

    private BigDecimal price(Plot plot) {
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
        var gui = Presentation.gui();
        var slots = gui.slots("market");
        List<Plot> all = matching(player.getUniqueId(), requested);
        MarketView view = requested.withPage(Math.min(requested.page(), Math.max(0, (all.size() - 1) / slots.size())));
        var holder = new PlotGUIHolder("PLOT_MARKET", null);
        holder.setMarketView(view);
        Map<String, Object> variables = new HashMap<>();
        variables.put("page", view.page() + 1);
        variables.put("sort", Texts.label(view.sort()));
        variables.put("district_filter", view.district() == null ? Texts.raw("labels.all") : view.district());
        variables.put("type_filter", view.propertyType() == null ? Texts.raw("labels.all") : view.propertyType());
        var inventory = gui.create("market", holder, variables);
        var shown = all.stream().skip((long) view.page() * slots.size()).limit(slots.size()).toList();
        for (int index = 0; index < shown.size(); index++) {
            Plot plot = shown.get(index);
            var fields = GuiVariables.plot(plot, config);
            fields.put("price", config.formatMoney(price(plot)));
            fields.put("tax", config.formatMoney(taxes.calculateTax(plot)));
            fields.put("type_icon", Optional.ofNullable(plots.getIcon(plot)).map(Enum::name).orElse(gui.value("market.fallback-material")));
            var item = gui.entry("market", fields);
            GuiData.set(item, "plot", plot.getId());
            GuiData.set(item, "action", "plot-entry");
            inventory.setItem(slots.get(index), item);
        }
        Set<String> hidden = new HashSet<>();
        if (view.page() == 0) hidden.add("previous");
        if ((view.page() + 1L) * slots.size() >= all.size()) hidden.add("next");
        gui.buttons(inventory, "market", variables, hidden);
        player.openInventory(inventory);
    }

    public void navigate(Player player, MarketView view, int slot) {
        navigate(player, view, Presentation.gui().actionAt("market", slot));
    }

    public void navigate(Player player, MarketView view, String action) {
        if (action == null) return;
        MarketView next = switch (action) {
            case "previous" -> view.withPage(view.page() - 1);
            case "next" -> view.withPage(view.page() + 1);
            case "owned" -> MarketView.start(MarketView.Tab.OWNED);
            case "sale" -> MarketView.start(MarketView.Tab.SALE);
            case "rent" -> MarketView.start(MarketView.Tab.RENT);
            case "auction" -> MarketView.start(MarketView.Tab.AUCTION);
            case "sort" ->
                    new MarketView(view.tab(), 0, MarketView.Sort.values()[(view.sort().ordinal() + 1) % MarketView.Sort.values().length], view.district(), view.propertyType());
            case "district" ->
                    new MarketView(view.tab(), 0, view.sort(), cycle(view.district(), plots.getAllPlots().stream().map(Plot::getDistrictId).filter(Objects::nonNull).distinct().sorted().toList()), view.propertyType());
            case "type" ->
                    new MarketView(view.tab(), 0, view.sort(), view.district(), cycle(view.propertyType(), plots.getAllPlots().stream().map(Plot::getPropertyType).distinct().sorted().toList()));
            default -> view;
        };
        open(player, next);
    }

    private String cycle(String current, List<String> values) {
        int next = values.indexOf(current) + 1;
        return next < values.size() ? values.get(next) : null;
    }

    public static String rentalTerms(Plot plot) {
        return plot.getRentalPrice() + ":" + plot.getRentalDeposit() + ":" + plot.getRentalPeriodUnit() + ":" + Settings.integer("rental.default-periods");
    }

    public void confirmRental(Player player, Plot plot, boolean autoRenew) {
        var gui = Presentation.gui();
        var holder = new PlotGUIHolder("PLOT_CONFIRM_RENT", plot.getId());
        holder.setAutoRenew(autoRenew);
        int periods = Settings.integer("rental.default-periods");
        var cost = plot.getRentalPrice().multiply(BigDecimal.valueOf(periods)).add(plot.getRentalDeposit());
        var variables = GuiVariables.plot(plot, config);
        variables.put("cost", config.formatMoney(cost));
        variables.put("tax", config.formatMoney(taxes.calculateTax(plot)));
        variables.put("autorenew", Texts.label(autoRenew));
        variables.put("rental_periods", periods);
        var inventory = gui.create("rental", holder, variables);
        gui.buttons(inventory, "rental", variables, Set.of());
        var confirm = gui.enabled("rental", "confirm") ? inventory.getItem(gui.number("rental.buttons.confirm.slot")) : null;
        if (confirm != null && "confirm".equals(GuiData.get(confirm, "action"))) {
            GuiData.set(confirm, "quote", cost.toPlainString());
            GuiData.set(confirm, "terms", rentalTerms(plot));
        }
        player.openInventory(inventory);
    }

    public void auction(Player player, Plot plot) {
        if (auctions == null) return;
        var auction = auctions.getAuction(plot.getId()).orElse(null);
        if (auction == null) {
            open(player, MarketView.start(MarketView.Tab.AUCTION));
            return;
        }
        var gui = Presentation.gui();
        var holder = new PlotGUIHolder("PLOT_AUCTION", plot.getId());
        var minimum = auction.getCurrentBidderUuid() == null ? auction.getStartingPrice() : auction.getCurrentBid().add(auction.getMinBidIncrement());
        if (auction.getBuyoutPrice() != null) minimum = minimum.min(auction.getBuyoutPrice());
        var reserved = auction.isEscrowEnabled() && player.getUniqueId().equals(auction.getCurrentBidderUuid()) ? auction.getCurrentBid() : BigDecimal.ZERO;
        var variables = GuiVariables.plot(plot, config);
        variables.put("minimum", config.formatMoney(minimum));
        variables.put("tax", config.formatMoney(taxes.calculateTax(plot)));
        variables.put("commission", config.getAuctionCityFeePercent());
        variables.put("end", pl.riftplots.util.TimeUtil.formatInstant(auction.getEndTime()));
        variables.put("charge", config.formatMoney(minimum.subtract(reserved)));
        variables.put("settlement", gui.render(gui.value(auction.isEscrowEnabled() ? "auction.settlement-reserved" : "auction.settlement-final"), variables));
        var history = new ArrayList<>(auction.getBids());
        Collections.reverse(history);
        variables.put("entries", history.stream().limit(gui.number("auction.history-lines")).map(bid -> gui.render(gui.value("auction.bid-line"), Map.of("bidder", bid.getBidderName(), "amount", config.formatMoney(bid.getAmount())))).toList());
        if (auction.getBuyoutPrice() != null) {
            variables.put("buyout", config.formatMoney(auction.getBuyoutPrice()));
            variables.put("buyout_charge", config.formatMoney(auction.getBuyoutPrice().subtract(reserved)));
        }
        var inventory = gui.create("auction", holder, variables);
        gui.buttons(inventory, "auction", variables, auction.getBuyoutPrice() == null ? Set.of("buyout") : Set.of());
        for (String action : List.of("bid", "buyout")) {
            if (!gui.enabled("auction", action)) continue;
            var item = inventory.getItem(gui.number("auction.buttons." + action + ".slot"));
            if (item != null && action.equals(GuiData.get(item, "action"))) {
                GuiData.set(item, "auction", auction.getId());
                GuiData.set(item, "quote", (action.equals("bid") ? minimum : auction.getBuyoutPrice()).toPlainString());
            }
        }
        player.openInventory(inventory);
    }
}
