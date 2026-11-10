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

    public List<Component> formatLines(PlotSign sign, Plot plot) {
        var variables = new HashMap<String, Object>();
        variables.put("plot_id", plot.getId());
        variables.put("plot_name", plot.getName());
        variables.put("owner", plot.getOwnerName() != null ? plot.getOwnerName() : pl.riftplots.config.Texts.raw("labels.none"));
        variables.put("status", pl.riftplots.config.Texts.label(plot.getStatus()));
        variables.put("price", config.formatMoney(Optional.ofNullable(plotManager.getPurchasePrice(plot)).orElse(plot.getPrice())));
        variables.put("tax", config.formatMoney(taxCalculator.calculateTax(plot)));
        variables.put("market", pl.riftplots.config.Texts.raw(plot.isAuctionActive() ? "labels.Tab.AUCTION" : "labels.Tab.RENT"));
        for (String name : List.of("header", "renew-header", "for-sale-action", "sold-action", "renew-action", "resetting-action", "disabled-action")) {
            variables.put(name.replace('-', '_'), pl.riftplots.util.AdventureUtil.parse(pl.riftplots.config.Settings.string("signs.formats." + name)));
        }
        String layout;
        if (sign.getType() == PlotSignType.RENEW) layout = "renew";
        else if (sign.getType() == PlotSignType.STATUS) layout = "status";
        else if (plot.isAvailableForPurchase()) layout = "sale";
        else if (plot.isAuctionActive() || plot.isRentalAvailable() || plot.isRented()) layout = "market";
        else if (plot.getStatus() == PlotStatus.RESETTING) layout = "resetting";
        else if (plot.getStatus() == PlotStatus.DISABLED) layout = "disabled";
        else layout = "owned";
        return pl.riftplots.config.Settings.strings("signs.layouts." + layout).stream()
                .map(line -> pl.riftplots.config.Presentation.gui().render(line, variables)).toList();
    }

    public void handleSignClick(Player player, PlotSign sign, boolean isRightClick) {
        if (player == null || sign == null || !config.isSignsEnabled()) {
            return;
        }
        Optional<Plot> optPlot = plotManager.getPlot(sign.getPlotId());
        if (optPlot.isEmpty()) {
            AdventureUtil.sendMessage(player, pl.riftplots.config.Texts.markup("text.plot-sign-service.powiazana-dziaka-nie-istnieje"));
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
                    paymentService.payTax(player, plot, pl.riftplots.config.Settings.integer("billing.default-payment-periods"));
                }
            } else if (plot.isAvailableForPurchase()) {
                if (paymentService != null && player.hasPermission("riftplots.user.buy")) {
                    paymentService.buyPlot(player, plot);
                }
            } else if (plot.isOwner(player.getUniqueId())) {
                if (paymentService != null && player.hasPermission("riftplots.user.pay")) {
                    paymentService.payTax(player, plot, pl.riftplots.config.Settings.integer("billing.default-payment-periods"));
                }
            } else {
                AdventureUtil.sendMessage(player, pl.riftplots.config.Texts.markup("text.plot-sign-service.ta-dziaka-nie-jest-na-sprzedaz"));
            }
        } else {
            displayPlotSummary(player, plot);
        }
    }

    public void displayPlotSummary(Player player, Plot plot) {
        AdventureUtil.sendMessage(player, pl.riftplots.config.Texts.markup("text.plot-sign-service.informacje-o-dziace"));
        AdventureUtil.sendMessage(player, pl.riftplots.config.Texts.markup("text.plot-sign-service.id", plot.getId()));
        AdventureUtil.sendMessage(player, pl.riftplots.config.Texts.markup("text.plot-sign-service.nazwa", plot.getName()));
        AdventureUtil.sendMessage(player, pl.riftplots.config.Texts.markup("text.plot-sign-service.status", plot.getStatus()));
        AdventureUtil.sendMessage(player, pl.riftplots.config.Texts.markup("text.plot-sign-service.wasciciel-2ec929c", (plot.getOwnerName() != null ? plot.getOwnerName() : pl.riftplots.config.Texts.raw("text.plot-sign-service.brak"))));
        AdventureUtil.sendMessage(player, pl.riftplots.config.Texts.markup("text.plot-sign-service.cena-zakupu", config.formatMoney(plotManager.getPurchasePrice(plot))));
        BigDecimal tax = taxCalculator.calculateTax(plot);
        AdventureUtil.sendMessage(player, pl.riftplots.config.Texts.markup("text.plot-sign-service.podatek", config.formatMoney(tax), plot.getTaxMode()));
        AdventureUtil.sendMessage(player, pl.riftplots.config.Texts.markup("text.plot-sign-service.powierzchnia-m2", plot.getBounds().getHorizontalArea()));
    }
}
