package pl.riftplots.service.auction;

import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import pl.riftplots.domain.auction.AuctionBid;
import pl.riftplots.domain.auction.AuctionStatus;
import pl.riftplots.domain.auction.PlotAuction;
import pl.riftplots.domain.city.CityTransactionType;
import pl.riftplots.domain.plot.Plot;
import pl.riftplots.domain.plot.PlotStatus;
import pl.riftplots.integration.vault.VaultHook;
import pl.riftplots.service.audit.AuditService;
import pl.riftplots.service.city.CityBudgetService;
import pl.riftplots.service.notification.NotificationService;
import pl.riftplots.service.plot.PlotManager;
import pl.riftplots.storage.sql.DatabaseManager;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionService {
    private final File auctionsFile;
    private final DatabaseManager databaseManager;
    private final PlotManager plotManager;
    private final VaultHook vaultHook;
    private final NotificationService notificationService;
    private final CityBudgetService budgetService;
    private final AuditService auditService;
    private final pl.riftplots.config.PluginConfiguration config;
    private final pl.riftplots.config.MessageConfiguration messageConfig;

    private final Map<String, PlotAuction> activeAuctions = new ConcurrentHashMap<>();

    public AuctionService(File dataFolder,
                          DatabaseManager databaseManager,
                          PlotManager plotManager,
                          VaultHook vaultHook,
                          NotificationService notificationService,
                          CityBudgetService budgetService,
                          AuditService auditService,
                          pl.riftplots.config.PluginConfiguration config,
                          pl.riftplots.config.MessageConfiguration messageConfig) {
        this.auctionsFile = new File(dataFolder, "data/auctions.yml");
        this.databaseManager = databaseManager;
        this.plotManager = plotManager;
        this.vaultHook = vaultHook;
        this.notificationService = notificationService;
        this.budgetService = budgetService;
        this.auditService = auditService;
        this.config = config;
        this.messageConfig = messageConfig;
    }

    public void initialize() {
        if (databaseManager != null) {
            loadFromDatabase();
        } else {
            loadFromFile();
        }
    }

    private void loadFromFile() {
        activeAuctions.clear();
        if (!auctionsFile.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(auctionsFile);
        var sec = config.getConfigurationSection("auctions");
        if (sec == null) return;

        for (String key : sec.getKeys(false)) {
            String p = "auctions." + key;
            String plotId = config.getString(p + ".plotId");
            String sStr = config.getString(p + ".sellerUuid");
            UUID sUuid = sStr != null && !sStr.isBlank() ? UUID.fromString(sStr) : null;
            BigDecimal startPrice = new BigDecimal(config.getString(p + ".startingPrice", "10000"));
            BigDecimal minInc = new BigDecimal(config.getString(p + ".minBidIncrement", "500"));
            BigDecimal curBid = new BigDecimal(config.getString(p + ".currentBid", "10000"));
            String bStr = config.getString(p + ".currentBidderUuid");
            UUID bUuid = bStr != null && !bStr.isBlank() ? UUID.fromString(bStr) : null;
            String bName = config.getString(p + ".currentBidderName");
            long start = config.getLong(p + ".startTime");
            long end = config.getLong(p + ".endTime");
            String buyStr = config.getString(p + ".buyoutPrice");
            BigDecimal buyout = buyStr != null && !buyStr.isBlank() ? new BigDecimal(buyStr) : null;
            AuctionStatus status = AuctionStatus.valueOf(config.getString(p + ".status", "ACTIVE"));

            PlotAuction auction = new PlotAuction(key, plotId, sUuid, startPrice, minInc, curBid, bUuid, bName, Instant.ofEpochMilli(start), Instant.ofEpochMilli(end), buyout, Collections.emptyList(), status);
            if (auction.getStatus() == AuctionStatus.ACTIVE) {
                activeAuctions.put(plotId.toLowerCase(), auction);
            }
        }
    }

    private void loadFromDatabase() {
        activeAuctions.clear();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM rift_auctions WHERE status = 'ACTIVE'");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String id = rs.getString("id");
                String plotId = rs.getString("plot_id");
                String sStr = rs.getString("seller_uuid");
                UUID sUuid = sStr != null && !sStr.isBlank() ? UUID.fromString(sStr) : null;
                BigDecimal startPrice = rs.getBigDecimal("starting_price");
                BigDecimal minInc = rs.getBigDecimal("min_bid_increment");
                BigDecimal curBid = rs.getBigDecimal("current_bid");
                String bStr = rs.getString("current_bidder_uuid");
                UUID bUuid = bStr != null && !bStr.isBlank() ? UUID.fromString(bStr) : null;
                String bName = rs.getString("current_bidder_name");
                long start = rs.getLong("start_time");
                long end = rs.getLong("end_time");
                BigDecimal buyout = rs.getBigDecimal("buyout_price");
                AuctionStatus status = AuctionStatus.valueOf(rs.getString("status"));

                PlotAuction auction = new PlotAuction(id, plotId, sUuid, startPrice, minInc, curBid, bUuid, bName, Instant.ofEpochMilli(start), Instant.ofEpochMilli(end), buyout, Collections.emptyList(), status);
                activeAuctions.put(plotId.toLowerCase(), auction);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public synchronized void saveAuction(PlotAuction auction) {
        if (auction == null) return;
        if (auction.getStatus() == AuctionStatus.ACTIVE) {
            activeAuctions.put(auction.getPlotId().toLowerCase(), auction);
        } else {
            activeAuctions.remove(auction.getPlotId().toLowerCase());
        }

        if (databaseManager != null) {
            String sql = """
                INSERT INTO rift_auctions (id, plot_id, seller_uuid, starting_price, min_bid_increment, current_bid, current_bidder_uuid, current_bidder_name, start_time, end_time, buyout_price, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET current_bid = excluded.current_bid, current_bidder_uuid = excluded.current_bidder_uuid, current_bidder_name = excluded.current_bidder_name, end_time = excluded.end_time, status = excluded.status
            """;
            if (databaseManager.getStorageType() == pl.riftplots.storage.StorageType.MYSQL) {
                sql = """
                    INSERT INTO rift_auctions (id, plot_id, seller_uuid, starting_price, min_bid_increment, current_bid, current_bidder_uuid, current_bidder_name, start_time, end_time, buyout_price, status)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE current_bid = VALUES(current_bid), current_bidder_uuid = VALUES(current_bidder_uuid), current_bidder_name = VALUES(current_bidder_name), end_time = VALUES(end_time), status = VALUES(status)
                """;
            }
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, auction.getId());
                ps.setString(2, auction.getPlotId());
                ps.setString(3, auction.getSellerUuid() != null ? auction.getSellerUuid().toString() : null);
                ps.setBigDecimal(4, auction.getStartingPrice());
                ps.setBigDecimal(5, auction.getMinBidIncrement());
                ps.setBigDecimal(6, auction.getCurrentBid());
                ps.setString(7, auction.getCurrentBidderUuid() != null ? auction.getCurrentBidderUuid().toString() : null);
                ps.setString(8, auction.getCurrentBidderName());
                ps.setLong(9, auction.getStartTime().toEpochMilli());
                ps.setLong(10, auction.getEndTime().toEpochMilli());
                ps.setBigDecimal(11, auction.getBuyoutPrice());
                ps.setString(12, auction.getStatus().name());
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            try {
                YamlConfiguration config = auctionsFile.exists() ? YamlConfiguration.loadConfiguration(auctionsFile) : new YamlConfiguration();
                String p = "auctions." + auction.getId();
                config.set(p + ".plotId", auction.getPlotId());
                config.set(p + ".sellerUuid", auction.getSellerUuid() != null ? auction.getSellerUuid().toString() : null);
                config.set(p + ".startingPrice", auction.getStartingPrice().toPlainString());
                config.set(p + ".minBidIncrement", auction.getMinBidIncrement().toPlainString());
                config.set(p + ".currentBid", auction.getCurrentBid().toPlainString());
                config.set(p + ".currentBidderUuid", auction.getCurrentBidderUuid() != null ? auction.getCurrentBidderUuid().toString() : null);
                config.set(p + ".currentBidderName", auction.getCurrentBidderName());
                config.set(p + ".startTime", auction.getStartTime().toEpochMilli());
                config.set(p + ".endTime", auction.getEndTime().toEpochMilli());
                config.set(p + ".buyoutPrice", auction.getBuyoutPrice() != null ? auction.getBuyoutPrice().toPlainString() : null);
                config.set(p + ".status", auction.getStatus().name());
                auctionsFile.getParentFile().mkdirs();
                config.save(auctionsFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized boolean startAuction(Plot plot, UUID sellerUuid, BigDecimal startingPrice, BigDecimal minIncrement, int durationMinutes, BigDecimal buyoutPrice) {
        if (plot == null || activeAuctions.containsKey(plot.getId().toLowerCase())) {
            return false;
        }

        PlotAuction auction = PlotAuction.create(plot.getId(), sellerUuid, startingPrice, minIncrement, durationMinutes, buyoutPrice);
        plot.setAuctionActive(true);
        plotManager.savePlot(plot);

        saveAuction(auction);
        auditService.log(plot.getId(), sellerUuid, "SYSTEM", "AUCTION_STARTED", "Cena wywoławcza: " + startingPrice + ", Czas: " + durationMinutes + "m");
        return true;
    }

    public synchronized boolean placeBid(Player bidder, String plotId, BigDecimal amount) {
        if (bidder == null || plotId == null) return false;
        PlotAuction auction = activeAuctions.get(plotId.toLowerCase());
        if (auction == null || auction.getStatus() != AuctionStatus.ACTIVE) {
            bidder.sendMessage(messageConfig.get("auction.not-active"));
            return false;
        }

        if (!vaultHook.has(bidder, amount)) {
            bidder.sendMessage(messageConfig.get("auction.cannot-afford", Map.of("amount", config.formatMoney(amount))));
            return false;
        }

        boolean bidPlaced = auction.placeBid(bidder.getUniqueId(), bidder.getName(), amount, config.getAuctionAntiSnipeSeconds());
        if (!bidPlaced) {
            BigDecimal minReq = auction.getCurrentBidderUuid() == null ? auction.getStartingPrice() : auction.getCurrentBid().add(auction.getMinBidIncrement());
            bidder.sendMessage(messageConfig.get("auction.min-bid-error", Map.of("min", config.formatMoney(minReq))));
            return false;
        }

        saveAuction(auction);
        notificationService.notifyPlayer(bidder.getUniqueId(), messageConfig.getRaw("auction.bid-accepted", "{prefix}<green>Oferta zostala przyjeta!</green>")
                .replace("{prefix}", config.getPrefix())
                .replace("{amount}", config.formatMoney(amount))
                .replace("{plot_id}", plotId), Sound.BLOCK_NOTE_BLOCK_BELL);
        return true;
    }

    public void checkCompletedAuctions() {
        for (PlotAuction auction : activeAuctions.values()) {
            if (auction.isExpired()) {
                finalizeAuction(auction);
            }
        }
    }

    private synchronized void finalizeAuction(PlotAuction auction) {
        auction.setStatus(AuctionStatus.COMPLETED);
        saveAuction(auction);

        Optional<Plot> optPlot = plotManager.getPlot(auction.getPlotId());
        if (optPlot.isEmpty()) return;
        Plot plot = optPlot.get();
        plot.setAuctionActive(false);

        if (auction.getCurrentBidderUuid() == null) {
            plotManager.savePlot(plot);
            auditService.log(plot.getId(), null, "SYSTEM", "AUCTION_CLOSED_NO_BIDS", "Brak ofert w licytacji.");
            return;
        }

        org.bukkit.OfflinePlayer winner = org.bukkit.Bukkit.getOfflinePlayer(auction.getCurrentBidderUuid());
        BigDecimal winningBid = auction.getCurrentBid();

        if (vaultHook.has(winner, winningBid)) {
            vaultHook.withdraw(winner, winningBid);

            double feePct = config.getAuctionCityFeePercent() / 100.0;
            if (auction.getSellerUuid() != null) {
                org.bukkit.OfflinePlayer seller = org.bukkit.Bukkit.getOfflinePlayer(auction.getSellerUuid());
                BigDecimal sellerShare = winningBid.multiply(BigDecimal.valueOf(1.0 - feePct)).setScale(2, RoundingMode.HALF_UP);
                vaultHook.deposit(seller, sellerShare);
                BigDecimal cityFee = winningBid.subtract(sellerShare);
                budgetService.recordTransaction(CityTransactionType.AUCTION_FEE, cityFee, plot.getId(), winner.getUniqueId(), "Prowizja miejska z aukcji");
            } else {
                budgetService.recordTransaction(CityTransactionType.PURCHASE_INCOME, winningBid, plot.getId(), winner.getUniqueId(), "Wygrana w licytacji działki miejskiej");
            }

            plot.setOwnerUuid(winner.getUniqueId());
            plot.setOwnerName(winner.getName());
            plot.setStatus(PlotStatus.OWNED);
            plot.setPurchaseDate(Instant.now());
            plotManager.savePlot(plot);

            notificationService.notifyPlayer(winner.getUniqueId(), messageConfig.getRaw("auction.won", "{prefix}<green>Wygrales licytacje!</green>")
                    .replace("{prefix}", config.getPrefix())
                    .replace("{name}", plot.getName())
                    .replace("{amount}", config.formatMoney(winningBid)), Sound.UI_TOAST_CHALLENGE_COMPLETE);
            auditService.log(plot.getId(), winner.getUniqueId(), winner.getName(), "AUCTION_WON", "Kwota: " + winningBid);
        } else {
            plotManager.savePlot(plot);
            auditService.log(plot.getId(), winner.getUniqueId(), winner.getName(), "AUCTION_PAYMENT_FAILED", "Brak środków u zwycięzcy");
        }
                budgetService.recordTransaction(CityTransactionType.AUCTION_FEE, cityFee, plot.getId(), winner.getUniqueId(), "Prowizja miejska z aukcji");
            } else {
                budgetService.recordTransaction(CityTransactionType.PURCHASE_INCOME, winningBid, plot.getId(), winner.getUniqueId(), "Wygrana w licytacji działki miejskiej");
            }

            plot.setOwnerUuid(winner.getUniqueId());
            plot.setOwnerName(winner.getName());
            plot.setStatus(PlotStatus.OWNED);
            plot.setPurchaseDate(Instant.now());
            plotManager.savePlot(plot);

            notificationService.notifyPlayer(winner.getUniqueId(), "<green>Wygrałeś licytację działki <gold>" + plot.getName() + "</gold> za kwotę <yellow>" + winningBid + " $</yellow>!</green>", Sound.UI_TOAST_CHALLENGE_COMPLETE);
            auditService.log(plot.getId(), winner.getUniqueId(), winner.getName(), "AUCTION_WON", "Kwota: " + winningBid);
        } else {
            plotManager.savePlot(plot);
            auditService.log(plot.getId(), winner.getUniqueId(), winner.getName(), "AUCTION_PAYMENT_FAILED", "Brak środków u zwycięzcy");
        }
    }

    public Collection<PlotAuction> getActiveAuctions() {
        return Collections.unmodifiableCollection(activeAuctions.values());
    }

    public Optional<PlotAuction> getAuction(String plotId) {
        if (plotId == null) return Optional.empty();
        return Optional.ofNullable(activeAuctions.get(plotId.toLowerCase()));
    }
}
