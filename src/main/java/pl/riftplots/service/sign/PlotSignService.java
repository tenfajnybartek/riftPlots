package pl.riftplots.service.sign;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import pl.riftplots.config.PluginConfiguration;
import pl.riftplots.domain.plot.Plot;
import pl.riftplots.domain.plot.PlotStatus;
import pl.riftplots.domain.sign.PlotSign;
import pl.riftplots.domain.sign.PlotSignType;
import pl.riftplots.service.payment.PaymentService;
import pl.riftplots.service.payment.TaxCalculator;
import pl.riftplots.service.plot.PlotManager;
import pl.riftplots.storage.repository.PlotSignRepository;
import pl.riftplots.util.AdventureUtil;

import java.math.BigDecimal;
import java.util.*;

public class PlotSignService {
    private final PluginConfiguration config;
    private final PlotSignRepository signRepository;
    private final PlotManager plotManager;
    private final TaxCalculator taxCalculator;
    private PaymentService paymentService;

    public PlotSignService(PluginConfiguration config,
                           PlotSignRepository signRepository,
                           PlotManager plotManager,
                           TaxCalculator taxCalculator) {
        this.config = config;
        this.signRepository = signRepository;
        this.plotManager = plotManager;
        this.taxCalculator = taxCalculator;
    }

    public void setPaymentService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    public List<PlotSign> getAllSigns() {
        return signRepository.findAll();
    }

    public Optional<PlotSign> getSignAt(String worldName, int x, int y, int z) {
        return signRepository.findByLocation(worldName, x, y, z);
    }

    public List<PlotSign> getSignsForPlot(String plotId) {
        return signRepository.findByPlotId(plotId);
    }

    public PlotSign registerSign(String plotId, PlotSignType type, String worldName, int x, int y, int z) {
        signRepository.findByLocation(worldName, x, y, z).ifPresent(existing -> signRepository.delete(existing.getId()));
        String id = UUID.randomUUID().toString();
        PlotSign sign = new PlotSign(id, plotId, type, worldName, x, y, z);
        signRepository.save(sign);
        updateSign(sign);
        return sign;
    }

    public void removeSign(String id) {
        signRepository.delete(id);
    }

    public void removeSignsForPlot(String plotId) {
        signRepository.deleteByPlotId(plotId);
    }

    public void updateAllSigns() {
        if (!config.isSignsEnabled()) {
            return;
        }
        for (PlotSign sign : signRepository.findAll()) {
            updateSign(sign);
        }
    }

    public void updateSignsForPlot(Plot plot) {
        if (!config.isSignsEnabled() || !config.isSignAutoUpdate() || plot == null) {
            return;
        }
        List<PlotSign> signs = signRepository.findByPlotId(plot.getId());
        for (PlotSign sign : signs) {
            updateSign(sign, plot);
        }
    }

    public void updateSign(PlotSign sign) {
        if (sign == null) {
            return;
        }
        Optional<Plot> optPlot = plotManager.getPlot(sign.getPlotId());
        optPlot.ifPresent(plot -> updateSign(sign, plot));
    }

    public void updateSign(PlotSign sign, Plot plot) {
        if (!config.isSignsEnabled() || sign == null || plot == null) {
            return;
        }
        World world = Bukkit.getWorld(sign.getWorldName());
        if (world == null) {
            return;
        }
        Block block = world.getBlockAt(sign.getX(), sign.getY(), sign.getZ());
        if (!(block.getState() instanceof Sign blockSign)) {
            return;
        }

        List<Component> lines = formatLines(sign, plot);
        for (int i = 0; i < 4; i++) {
            Component line = i < lines.size() ? lines.get(i) : Component.empty();
            blockSign.getSide(Side.FRONT).line(i, line);
        }
        blockSign.update(true, false);
    }

    private List<Component> formatLines(PlotSign sign, Plot plot) {
        List<Component> lines = new ArrayList<>(4);
        PlotStatus status = plot.getStatus();

        if (sign.getType() == PlotSignType.FOR_SALE) {
            lines.add(AdventureUtil.parse(config.getSignHeader()));
            lines.add(AdventureUtil.parse("<dark_gray>" + plot.getId() + "</dark_gray>"));
            if (plot.isAvailableForPurchase()) {
                lines.add(AdventureUtil.parse("<gold>" + config.formatMoney(plot.getPrice()) + "</gold>"));
                lines.add(AdventureUtil.parse(config.getSignForSaleAction()));
            } else if (plot.isAuctionActive() || plot.isRentalAvailable() || plot.isRented()) {
                lines.add(AdventureUtil.parse("<gray>" + (plot.isAuctionActive() ? "Aukcja" : "Wynajem") + "</gray>"));
                lines.add(AdventureUtil.parse(config.getSignResettingAction()));
            } else if (status == PlotStatus.RESETTING) {
                lines.add(AdventureUtil.parse("<gray>Resetowanie...</gray>"));
                lines.add(AdventureUtil.parse(config.getSignResettingAction()));
            } else if (status == PlotStatus.DISABLED) {
                lines.add(AdventureUtil.parse("<dark_gray>Wyłączona</dark_gray>"));
                lines.add(AdventureUtil.parse(config.getSignDisabledAction()));
            } else {
                lines.add(AdventureUtil.parse("<yellow>" + (plot.getOwnerName() != null ? plot.getOwnerName() : "Właściciel") + "</yellow>"));
                lines.add(AdventureUtil.parse(config.getSignSoldAction()));
            }
        } else if (sign.getType() == PlotSignType.RENEW) {
            lines.add(AdventureUtil.parse(config.getSignRenewHeader()));
            lines.add(AdventureUtil.parse("<dark_gray>" + plot.getId() + "</dark_gray>"));
            BigDecimal tax = taxCalculator.calculateTax(plot);
            lines.add(AdventureUtil.parse("<gold>" + config.formatMoney(tax) + "</gold>"));
            lines.add(AdventureUtil.parse(config.getSignRenewAction()));
        } else {
            lines.add(AdventureUtil.parse("<gradient:#00c6ff:#0072ff><b>" + plot.getName() + "</b></gradient>"));
            lines.add(AdventureUtil.parse("<dark_gray>" + plot.getId() + "</dark_gray>"));
            lines.add(AdventureUtil.parse("<yellow>" + (plot.getOwnerName() != null ? plot.getOwnerName() : "Brak") + "</yellow>"));
            String statusStr = switch (status) {
                case AVAILABLE -> "<green>DOSTĘPNA</green>";
                case OWNED -> "<dark_green>OPŁACONA</dark_green>";
                case PAYMENT_DUE -> "<gold>ZALEGŁOŚĆ</gold>";
                case GRACE_PERIOD -> "<red>KARENCJA</red>";
                case RESETTING -> "<gray>RESET</gray>";
                case DISABLED -> "<dark_red>WYŁĄCZONA</dark_red>";
            };
            lines.add(AdventureUtil.parse(statusStr));
        }
        return lines;
    }

    public void handleSignClick(Player player, PlotSign sign, boolean isRightClick) {
        if (player == null || sign == null || !config.isSignsEnabled()) {
            return;
        }
        Optional<Plot> optPlot = plotManager.getPlot(sign.getPlotId());
        if (optPlot.isEmpty()) {
            AdventureUtil.sendMessage(player, "<red>Powiązana działka nie istnieje!</red>");
            return;
        }
        Plot plot = optPlot.get();
        if (!player.hasPermission("riftplots.user")) return;
        if (sign.getType() == PlotSignType.STATUS) {
            displayPlotSummary(player, plot);
            return;
        }

        if (isRightClick) {
            if (sign.getType() == PlotSignType.RENEW) {
                if (paymentService != null && player.hasPermission("riftplots.user.pay")) {
                    paymentService.payTax(player, plot, 1);
                }
            } else if (plot.isAvailableForPurchase()) {
                if (paymentService != null && player.hasPermission("riftplots.user.buy")) {
                    paymentService.buyPlot(player, plot);
                }
            } else if (plot.isOwner(player.getUniqueId())) {
                if (paymentService != null && player.hasPermission("riftplots.user.pay")) {
                    paymentService.payTax(player, plot, 1);
                }
            } else {
                AdventureUtil.sendMessage(player, "<red>Ta działka nie jest na sprzedaż!</red>");
            }
        } else {
            displayPlotSummary(player, plot);
        }
    }

    public void displayPlotSummary(Player player, Plot plot) {
        AdventureUtil.sendMessage(player, "<gradient:#00c6ff:#0072ff><b>=== INFORMACJE O DZIAŁCE ===</b></gradient>");
        AdventureUtil.sendMessage(player, "<gray>ID:</gray> <white>" + plot.getId() + "</white>");
        AdventureUtil.sendMessage(player, "<gray>Nazwa:</gray> <gold>" + plot.getName() + "</gold>");
        AdventureUtil.sendMessage(player, "<gray>Status:</gray> <yellow>" + plot.getStatus() + "</yellow>");
        AdventureUtil.sendMessage(player, "<gray>Właściciel:</gray> <aqua>" + (plot.getOwnerName() != null ? plot.getOwnerName() : "Brak") + "</aqua>");
        AdventureUtil.sendMessage(player, "<gray>Cena zakupu:</gray> <green>" + config.formatMoney(plot.getPrice()) + "</green>");
        BigDecimal tax = taxCalculator.calculateTax(plot);
        AdventureUtil.sendMessage(player, "<gray>Podatek:</gray> <yellow>" + config.formatMoney(tax) + " (" + plot.getTaxMode() + ")</yellow>");
        AdventureUtil.sendMessage(player, "<gray>Powierzchnia:</gray> <white>" + plot.getBounds().getHorizontalArea() + " m²</white>");
    }
}
