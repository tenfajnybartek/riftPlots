package pl.riftplots.domain.district;

import java.math.BigDecimal;
import java.util.*;

public class District {
    private final String id;
    private String name;
    private String description;
    private double priceMultiplier;
    private double taxMultiplier;
    private final Set<String> allowedPropertyTypes = new HashSet<>();
    private int maxPlotsPerPlayer;
    private boolean salesEnabled;
    private boolean rentalEnabled;

    public District(String id,
                    String name,
                    String description,
                    double priceMultiplier,
                    double taxMultiplier,
                    Set<String> allowedTypes,
                    int maxPlotsPerPlayer,
                    boolean salesEnabled,
                    boolean rentalEnabled) {
        this.id = Objects.requireNonNull(id, "id cannot be null").toLowerCase();
        this.name = name != null ? name : id;
        this.description = description != null ? description : "";
        this.priceMultiplier = priceMultiplier > 0 ? priceMultiplier : 1.0;
        this.taxMultiplier = taxMultiplier > 0 ? taxMultiplier : 1.0;
        if (allowedTypes != null) {
            for (String t : allowedTypes) {
                this.allowedPropertyTypes.add(t.toUpperCase());
            }
        }
        this.maxPlotsPerPlayer = Math.max(0, maxPlotsPerPlayer);
        this.salesEnabled = salesEnabled;
        this.rentalEnabled = rentalEnabled;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPriceMultiplier() {
        return priceMultiplier;
    }

    public void setPriceMultiplier(double priceMultiplier) {
        this.priceMultiplier = priceMultiplier > 0 ? priceMultiplier : 1.0;
    }

    public double getTaxMultiplier() {
        return taxMultiplier;
    }

    public void setTaxMultiplier(double taxMultiplier) {
        this.taxMultiplier = taxMultiplier > 0 ? taxMultiplier : 1.0;
    }

    public Set<String> getAllowedPropertyTypes() {
        return Collections.unmodifiableSet(allowedPropertyTypes);
    }

    public void addAllowedType(String type) {
        if (type != null) {
            allowedPropertyTypes.add(type.toUpperCase());
        }
    }

    public void removeAllowedType(String type) {
        if (type != null) {
            allowedPropertyTypes.remove(type.toUpperCase());
        }
    }

    public boolean isTypeAllowed(String type) {
        if (allowedPropertyTypes.isEmpty()) {
            return true;
        }
        return type != null && allowedPropertyTypes.contains(type.toUpperCase());
    }

    public int getMaxPlotsPerPlayer() {
        return maxPlotsPerPlayer;
    }

    public void setMaxPlotsPerPlayer(int maxPlotsPerPlayer) {
        this.maxPlotsPerPlayer = Math.max(0, maxPlotsPerPlayer);
    }

    public boolean isSalesEnabled() {
        return salesEnabled;
    }

    public void setSalesEnabled(boolean salesEnabled) {
        this.salesEnabled = salesEnabled;
    }

    public boolean isRentalEnabled() {
        return rentalEnabled;
    }

    public void setRentalEnabled(boolean rentalEnabled) {
        this.rentalEnabled = rentalEnabled;
    }
}
