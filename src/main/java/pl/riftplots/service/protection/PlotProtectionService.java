package pl.riftplots.service.protection;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import pl.riftplots.config.PermissionConfiguration;
import pl.riftplots.domain.plot.Plot;
import pl.riftplots.domain.plot.PlotPermission;
import pl.riftplots.domain.plot.PlotStatus;
import pl.riftplots.service.plot.PlotManager;

import java.util.Optional;

public class PlotProtectionService {
    private final PlotManager plotManager;

    public PlotProtectionService(PlotManager plotManager) {
        this.plotManager = plotManager;
    }

    public boolean canInteract(Player player, Location location, PlotPermission requiredPermission) {
        if (location == null || location.getWorld() == null) {
            return true;
        }

        Optional<Plot> optPlot = plotManager.getPlotAt(
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ()
        );

        if (optPlot.isEmpty()) {
            return true;
        }

        Plot plot = optPlot.get();

        if (player == null) {
            return false;
        }

        if (player.hasPermission(PermissionConfiguration.ADMIN_BYPASS)) {
            return true;
        }

        if (plot.getStatus() == PlotStatus.RESETTING || plot.getStatus() == PlotStatus.DISABLED) {
            return false;
        }

        if (plot.getStatus() == PlotStatus.AVAILABLE) {
            return false;
        }

        if (plot.isOwner(player.getUniqueId())) {
            return true;
        }

        if (requiredPermission != null) {
            return plot.hasPermission(player.getUniqueId(), requiredPermission);
        }

        return false;
    }
}
