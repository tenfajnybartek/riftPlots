package pl.riftplots.gui;

import pl.riftplots.config.*;
import pl.riftplots.domain.plot.Plot;
import pl.riftplots.util.TimeUtil;

import java.util.*;

final class GuiVariables {
    private GuiVariables() {
    }

    static Map<String, Object> plot(Plot plot, PluginConfiguration config) {
        Map<String, Object> result = new HashMap<>();
        result.put("plot_id", plot.getId());
        result.put("plot_name", plot.getName());
        result.put("status", Texts.label(plot.getStatus()));
        result.put("world", plot.getBounds().getWorldName());
        result.put("width", plot.getBounds().getWidthX());
        result.put("length", plot.getBounds().getLengthZ());
        result.put("height", plot.getBounds().getHeightY());
        result.put("area", plot.getBounds().getHorizontalArea());
        result.put("volume", plot.getBounds().getVolume());
        result.put("price", config.formatMoney(plot.getPrice()));
        result.put("tax", config.formatMoney(plot.getBaseTax()));
        result.put("period", plot.getBillingPeriodDays());
        result.put("advance_paid", plot.getAdvancePeriodsPaid());
        result.put("advance_max", config.getMaxAdvancePeriods());
        result.put("member_count", plot.getMembers().size());
        result.put("purchase_date", TimeUtil.formatInstant(plot.getPurchaseDate()));
        result.put("next_tax", TimeUtil.formatInstant(plot.getNextTaxPayment()));
        result.put("type", plot.getPropertyType());
        result.put("district", plot.getDistrictId() == null ? Texts.raw("labels.none") : plot.getDistrictId());
        result.put("rent", config.formatMoney(plot.getRentalPrice()));
        result.put("deposit", config.formatMoney(plot.getRentalDeposit()));
        result.put("rental_unit", Texts.raw("labels.RentalPeriodUnit." + plot.getRentalPeriodUnit()));
        result.put("owner", plot.getOwnerName() == null ? Texts.raw("labels.none") : plot.getOwnerName());
        return result;
    }
}
