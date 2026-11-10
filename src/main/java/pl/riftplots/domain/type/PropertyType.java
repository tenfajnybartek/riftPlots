package pl.riftplots.domain.type;

import org.bukkit.Material;
import pl.riftplots.domain.plot.TaxMode;

import java.math.BigDecimal;
import java.util.*;

public class PropertyType {
    private final String id;
    private String displayName;
    private String description;
    private BigDecimal defaultPrice;
    private BigDecimal defaultBaseTax;
    private TaxMode defaultTaxMode;
    private double defaultTaxMultiplier;
    private int defaultBillingPeriodDays;
    private int maxMembers;
    private boolean rentalAllowed;
    private boolean salesAllowed;
    private double refundPercent;
    private Material iconMaterial;
    private final Map<String, String> defaultFlags = new HashMap<>();

    public PropertyType(String id,
                        String displayName,
                        String description,
                        BigDecimal defaultPrice,
                        BigDecimal defaultBaseTax,
                        TaxMode defaultTaxMode,
                        double defaultTaxMultiplier,
                        int defaultBillingPeriodDays,
                        int maxMembers,
                        boolean rentalAllowed,
                        boolean salesAllowed,
                        double refundPercent,
                        Material iconMaterial,
                        Map<String, String> defaultFlags) {
        this.id = Objects.requireNonNull(id, pl.riftplots.config.Texts.raw("text.property-type.id-cannot-be-null")).toUpperCase();
        this.displayName = displayName != null ? displayName : id;
        this.description = description != null ? description : "";
        this.defaultPrice = defaultPrice != null ? defaultPrice : BigDecimal.valueOf(pl.riftplots.config.Settings.decimal("economy.default-price"));
        this.defaultBaseTax = defaultBaseTax != null ? defaultBaseTax : BigDecimal.valueOf(pl.riftplots.config.Settings.decimal("economy.default-base-tax"));
        this.defaultTaxMode = defaultTaxMode != null ? defaultTaxMode : TaxMode.parse(pl.riftplots.config.Settings.string("economy.default-tax-mode"));
        this.defaultTaxMultiplier = defaultTaxMultiplier > 0 ? defaultTaxMultiplier : 1.0;
        this.defaultBillingPeriodDays = defaultBillingPeriodDays > 0 ? defaultBillingPeriodDays : pl.riftplots.config.Settings.integer("economy.default-billing-period-days");
        this.maxMembers = Math.max(0, maxMembers);
        this.rentalAllowed = rentalAllowed;
        this.salesAllowed = salesAllowed;
        this.refundPercent = refundPercent >= 0 ? refundPercent : pl.riftplots.config.Settings.decimal("economy.default-abandon-refund-percent");
        this.iconMaterial = iconMaterial != null ? iconMaterial : Material.matchMaterial(pl.riftplots.config.Settings.string("plots.default-icon"));
        if (defaultFlags != null) {
            this.defaultFlags.putAll(defaultFlags);
        }
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getDefaultPrice() {
        return defaultPrice;
    }

    public void setDefaultPrice(BigDecimal defaultPrice) {
        this.defaultPrice = defaultPrice;
    }

    public BigDecimal getDefaultBaseTax() {
        return defaultBaseTax;
    }

    public void setDefaultBaseTax(BigDecimal defaultBaseTax) {
        this.defaultBaseTax = defaultBaseTax;
    }

    public TaxMode getDefaultTaxMode() {
        return defaultTaxMode;
    }

    public void setDefaultTaxMode(TaxMode defaultTaxMode) {
        this.defaultTaxMode = defaultTaxMode;
    }

    public double getDefaultTaxMultiplier() {
        return defaultTaxMultiplier;
    }

    public void setDefaultTaxMultiplier(double defaultTaxMultiplier) {
        this.defaultTaxMultiplier = defaultTaxMultiplier;
    }

    public int getDefaultBillingPeriodDays() {
        return defaultBillingPeriodDays;
    }

    public void setDefaultBillingPeriodDays(int defaultBillingPeriodDays) {
        this.defaultBillingPeriodDays = defaultBillingPeriodDays;
    }

    public int getMaxMembers() {
        return maxMembers;
    }

    public void setMaxMembers(int maxMembers) {
        this.maxMembers = maxMembers;
    }

    public boolean isRentalAllowed() {
        return rentalAllowed;
    }

    public void setRentalAllowed(boolean rentalAllowed) {
        this.rentalAllowed = rentalAllowed;
    }

    public boolean isSalesAllowed() {
        return salesAllowed;
    }

    public void setSalesAllowed(boolean salesAllowed) {
        this.salesAllowed = salesAllowed;
    }

    public double getRefundPercent() {
        return refundPercent;
    }

    public void setRefundPercent(double refundPercent) {
        this.refundPercent = refundPercent;
    }

    public Material getIconMaterial() {
        return iconMaterial;
    }

    public void setIconMaterial(Material iconMaterial) {
        this.iconMaterial = iconMaterial;
    }

    public Map<String, String> getDefaultFlags() {
        return Collections.unmodifiableMap(defaultFlags);
    }
}
