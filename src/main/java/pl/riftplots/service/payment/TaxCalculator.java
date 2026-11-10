package pl.riftplots.service.payment;

import pl.riftplots.domain.district.District;
import pl.riftplots.domain.plot.Plot;
import pl.riftplots.domain.plot.PlotBounds;
import pl.riftplots.domain.plot.TaxMode;
import pl.riftplots.service.district.DistrictManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

public class TaxCalculator {
    private final DistrictManager districtManager;

    public TaxCalculator() {
        this.districtManager = null;
    }

    public TaxCalculator(DistrictManager districtManager) {
        this.districtManager = districtManager;
    }

    public BigDecimal calculateTax(Plot plot) {
        if (plot == null || !plot.isTaxEnabled()) {
            return BigDecimal.ZERO;
        }

        BigDecimal base = plot.getBaseTax() != null ? plot.getBaseTax() : BigDecimal.ZERO;
        if (base.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        PlotBounds bounds = plot.getBounds();
        TaxMode mode = plot.getTaxMode() != null ? plot.getTaxMode() : TaxMode.FIXED;

        BigDecimal calculated;
        switch (mode) {
            case FIXED, CUSTOM -> calculated = base;
            case MULTIPLIER -> {
                double multiplier = Math.max(pl.riftplots.config.Settings.decimal("tax.minimum-multiplier"), plot.getTaxMultiplier());
                calculated = base.multiply(BigDecimal.valueOf(multiplier));
            }
            case PER_BLOCK -> {
                long area = bounds != null ? bounds.getHorizontalArea() : 1L;
                calculated = base.multiply(BigDecimal.valueOf(area));
            }
            case PER_CHUNK -> {
                int chunks = bounds != null ? bounds.getChunksCount() : 1;
                calculated = base.multiply(BigDecimal.valueOf(chunks));
            }
            case SIZE_TIERS -> {
                long area = bounds != null ? bounds.getHorizontalArea() : 1L;
                double tierMultiplier = pl.riftplots.config.Settings.decimal("tax.size-tier-overflow-multiplier");
                var settings = pl.riftplots.config.Settings.section("tax");
                var limits = settings.getLongList("size-tier-limits");
                var multipliers = settings.getDoubleList("size-tier-multipliers");
                for (int index = 0; index < limits.size(); index++) {
                    if (area <= limits.get(index)) {
                        tierMultiplier = multipliers.get(index);
                        break;
                    }
                }
                calculated = base.multiply(BigDecimal.valueOf(tierMultiplier));
            }
            default -> calculated = base;
        }

        if (districtManager != null && plot.getDistrictId() != null) {
            Optional<District> dist = districtManager.getDistrict(plot.getDistrictId());
            if (dist.isPresent()) {
                calculated = calculated.multiply(BigDecimal.valueOf(dist.get().getTaxMultiplier()));
            }
        }

        return calculated.setScale(pl.riftplots.config.Settings.integer("economy.decimal-places"), pl.riftplots.config.Settings.rounding()).max(BigDecimal.ZERO);
    }
}
