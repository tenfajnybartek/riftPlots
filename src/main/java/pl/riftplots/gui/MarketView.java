package pl.riftplots.gui;

public record MarketView(Tab tab, int page, Sort sort, String district, String propertyType) {
    public enum Tab {OWNED, SALE, RENT, AUCTION}

    public enum Sort {ID, PRICE, TAX}

    public static MarketView start(Tab tab) {
        return new MarketView(tab, 0, Sort.valueOf(pl.riftplots.config.Presentation.gui().value("market.default-sort")), null, null);
    }

    public MarketView withPage(int page) {
        return new MarketView(tab, Math.max(0, page), sort, district, propertyType);
    }
}
