package pl.riftplots.service.auction;

import org.bukkit.configuration.file.YamlConfiguration;
import pl.riftplots.storage.yaml.YamlDataFile;
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
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionService {
    private pl.riftplots.service.transaction.OperationJournal journal;

    public void setOperationJournal(pl.riftplots.service.transaction.OperationJournal journal) {
        this.journal = journal;
    }

    private final Map<String, PlotAuction> committed = new HashMap<>();
    private pl.riftplots.service.transaction.RefundService refunds;

    public void setRefundService(pl.riftplots.service.transaction.RefundService refunds) {
        this.refunds = refunds;
    }

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
        committed.clear();
        activeAuctions.values().forEach(auction -> committed.put(auction.getId(), auction.copy()));
        for (Plot plot : plotManager.getAllPlots()) {
            boolean active = activeAuctions.containsKey(plot.getId());
            if (plot.isAuctionActive() != active) {
                plot.setAuctionActive(active);
                plotManager.savePlot(plot);
            }
        }
    }

    private void loadFromFile() {
        activeAuctions.clear();
        if (!auctionsFile.exists()) return;
        YamlConfiguration config = YamlDataFile.load(auctionsFile);
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

            PlotAuction auction = new PlotAuction(key, plotId, sUuid, startPrice, minInc, curBid, bUuid, bName, Instant.ofEpochMilli(start), Instant.ofEpochMilli(end), buyout, fileBids(config, p, key), status);
            auction.setEscrowEnabled(config.getBoolean(p + ".escrow", false));
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

                PlotAuction auction = new PlotAuction(id, plotId, sUuid, startPrice, minInc, curBid, bUuid, bName, Instant.ofEpochMilli(start), Instant.ofEpochMilli(end), buyout, databaseBids(conn, id), status);
                auction.setEscrowEnabled(rs.getBoolean("escrow_enabled"));
                activeAuctions.put(plotId.toLowerCase(), auction);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(pl.riftplots.config.Texts.raw("text.auction-service.storage-operation-failed"), e);
        }
    }

    public synchronized void saveAuction(PlotAuction auction) {
        if (auction == null) return;
        try {
            persistAuction(auction);
        } catch (RuntimeException e) {
            PlotAuction previous = committed.get(auction.getId());
            if (previous != null) auction.restore(previous);
            throw e;
        }
        if (auction.getStatus() == AuctionStatus.ACTIVE) {
            activeAuctions.put(auction.getPlotId().toLowerCase(), auction);
        } else {
            activeAuctions.remove(auction.getPlotId().toLowerCase());
        }
        committed.put(auction.getId(), auction.copy());
    }

    private void persistAuction(PlotAuction auction) {
        if (journal != null && !journal.isWriteAllowed())
            throw new IllegalStateException(pl.riftplots.config.Texts.raw("text.auction-service.trwa-migracja-danych"));
        if (databaseManager != null) {
            String sql = """
                        INSERT INTO rift_auctions (id, plot_id, seller_uuid, starting_price, min_bid_increment, current_bid, current_bidder_uuid, current_bidder_name, start_time, end_time, buyout_price, status, escrow_enabled)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT(id) DO UPDATE SET current_bid = excluded.current_bid, current_bidder_uuid = excluded.current_bidder_uuid, current_bidder_name = excluded.current_bidder_name, end_time = excluded.end_time, status = excluded.status, escrow_enabled = excluded.escrow_enabled
                    """;
            if (databaseManager.getStorageType() == pl.riftplots.storage.StorageType.MYSQL) {
                sql = """
                            INSERT INTO rift_auctions (id, plot_id, seller_uuid, starting_price, min_bid_increment, current_bid, current_bidder_uuid, current_bidder_name, start_time, end_time, buyout_price, status, escrow_enabled)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            ON DUPLICATE KEY UPDATE current_bid = VALUES(current_bid), current_bidder_uuid = VALUES(current_bidder_uuid), current_bidder_name = VALUES(current_bidder_name), end_time = VALUES(end_time), status = VALUES(status), escrow_enabled = VALUES(escrow_enabled)
                        """;
            }
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                conn.setAutoCommit(false);
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
                ps.setBoolean(13, auction.isEscrowEnabled());
                ps.executeUpdate();
                persistBids(conn, auction);
                conn.commit();
            } catch (SQLException e) {
                throw new IllegalStateException(pl.riftplots.config.Texts.raw("text.auction-service.storage-operation-failed"), e);
            }
        } else {
            try {
                YamlConfiguration config = auctionsFile.exists() ? YamlDataFile.load(auctionsFile) : new YamlConfiguration();
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
                config.set(p + ".escrow", auction.isEscrowEnabled());
                config.set(p + ".bids", auction.getBids().stream().map(bid -> Map.of("bidderUuid", bid.getBidderUuid().toString(),
                        "bidderName", bid.getBidderName(), "amount", bid.getAmount().toPlainString(), "timestamp", bid.getTimestamp().toEpochMilli())).toList());
                auctionsFile.getParentFile().mkdirs();
                YamlDataFile.save(config, auctionsFile);
            } catch (IOException e) {
                throw new IllegalStateException(pl.riftplots.config.Texts.raw("text.auction-service.storage-operation-failed"), e);
            }
        }
    }

    public synchronized boolean startAuction(Plot plot, UUID sellerUuid, BigDecimal startingPrice, BigDecimal minIncrement, int durationMinutes, BigDecimal buyoutPrice) {
        if (plot == null) return false;
        return journal == null ? startAuctionInternal(plot, sellerUuid, startingPrice, minIncrement, durationMinutes, buyoutPrice)
                : journal.execute("AUCTION_START", "auction-start:" + plot.getId() + ":" + plot.getUpdatedAt().toEpochMilli(),
                plot.getId(), sellerUuid, startingPrice,
                () -> startAuctionInternal(plot, sellerUuid, startingPrice, minIncrement, durationMinutes, buyoutPrice));
    }

    private boolean startAuctionInternal(Plot plot, UUID sellerUuid, BigDecimal startingPrice, BigDecimal minIncrement, int durationMinutes, BigDecimal buyoutPrice) {
        if (plot == null || activeAuctions.containsKey(plot.getId().toLowerCase())
                || plot.isAuctionActive() || plot.isRented() || plot.isRentalAvailable() || plotManager.isResetPending(plot.getId())
                || (plot.getStatus() != PlotStatus.AVAILABLE && plot.getStatus() != PlotStatus.OWNED)
                || !Objects.equals(sellerUuid, plot.getOwnerUuid())
                || plotManager.financiallyBlocked(plot.getId(), plotManager.currentOperationId())
                || startingPrice == null || startingPrice.signum() < 0
                || minIncrement == null || minIncrement.signum() <= 0 || durationMinutes <= 0
                || (buyoutPrice != null && buyoutPrice.compareTo(startingPrice) < 0)) {
            return false;
        }

        PlotAuction auction = PlotAuction.create(plot.getId(), sellerUuid, startingPrice, minIncrement, durationMinutes, buyoutPrice);
        if (config.isAuctionEscrowEnabled() && (journal == null || refunds == null)) return false;
        auction.setEscrowEnabled(config.isAuctionEscrowEnabled());
        plot.setAuctionActive(true);
        plotManager.savePlot(plot);

        saveAuction(auction);
        auditService.log(plot.getId(), sellerUuid, pl.riftplots.config.Texts.raw("text.auction-service.system"), "AUCTION_STARTED", pl.riftplots.config.Texts.raw("text.auction-service.cena-wywoawcza-czas-m", startingPrice, durationMinutes));
        return true;
    }

    public synchronized boolean placeBid(Player bidder, String plotId, BigDecimal amount) {
        if (bidder == null || plotId == null || amount == null) return false;
        PlotAuction auction = activeAuctions.get(plotId.toLowerCase());
        if (auction == null) return false;
        boolean accepted = journal == null ? placeBidInternal(bidder, plotId, amount) : journal.execute("BID",
                "bid:" + auction.getId() + ":" + auction.getBids().size() + ":" + bidder.getUniqueId() + ":" + amount,
                plotId, bidder.getUniqueId(), amount, () -> placeBidInternal(bidder, plotId, amount));
        if (accepted && auction.isExpired()) finalizeAuction(auction);
        if (refunds != null) refunds.retryPending();
        return accepted;
    }

    private boolean placeBidInternal(Player bidder, String plotId, BigDecimal amount) {
        if (bidder == null || plotId == null || amount == null || amount.signum() < 0) return false;
        PlotAuction auction = activeAuctions.get(plotId.toLowerCase());
        if (auction == null || auction.getStatus() != AuctionStatus.ACTIVE || auction.isExpired()) {
            bidder.sendMessage(messageConfig.get("auction.not-active"));
            return false;
        }

        Optional<Plot> plot = plotManager.getPlot(plotId);
        if (plot.isEmpty() || !plotManager.validateAcquisition(plot.get(), bidder.getUniqueId()).isValid()
                || bidder.getUniqueId().equals(auction.getSellerUuid())) {
            bidder.sendMessage(messageConfig.get("transfer.not-ready"));
            return false;
        }

        PlotAuction previous = auction.copy();
        BigDecimal charge = auction.isEscrowEnabled() && bidder.getUniqueId().equals(previous.getCurrentBidderUuid())
                ? amount.subtract(previous.getCurrentBid()) : amount;
        if (!vaultHook.has(bidder, charge)) {
            bidder.sendMessage(messageConfig.get("auction.cannot-afford", Map.of("amount", config.formatMoney(amount))));
            return false;
        }

        boolean bidPlaced = auction.placeBid(bidder.getUniqueId(), bidder.getName(), amount, config.getAuctionAntiSnipeSeconds());
        if (!bidPlaced) {
            BigDecimal minReq = auction.getCurrentBidderUuid() == null ? auction.getStartingPrice() : auction.getCurrentBid().add(auction.getMinBidIncrement());
            bidder.sendMessage(messageConfig.get("auction.min-bid-error", Map.of("min", config.formatMoney(minReq))));
            return false;
        }

        if (auction.isEscrowEnabled()) {
            if (!vaultHook.withdraw(bidder, charge)) {
                auction.restore(previous);
                return false;
            }
            if (previous.getCurrentBidderUuid() != null && !bidder.getUniqueId().equals(previous.getCurrentBidderUuid())) {
                refunds.prepare("outbid:" + auction.getId() + ":" + previous.getBids().size(), "AUCTION_OUTBID", plotId,
                        journal.currentId(), previous.getCurrentBidderUuid(), previous.getCurrentBid(), pl.riftplots.config.Texts.raw("text.auction-service.zwrot-przebitej-oferty-aukcji", auction.getId()));
            }
        }
        saveAuction(auction);
        notificationService.notifyPlayer(bidder.getUniqueId(), messageConfig.getRaw("auction.bid-accepted")
                .replace("{prefix}", config.getPrefix())
                .replace("{amount}", config.formatMoney(amount))
                .replace("{plot_id}", plotId), pl.riftplots.config.Settings.string("sounds.auction-bid"));
        return true;
    }

    public void checkCompletedAuctions() {
        for (PlotAuction auction : activeAuctions.values()) {
            if (auction.isExpired()) {
                try {
                    finalizeAuction(auction);
                } catch (RuntimeException e) {
                    java.util.logging.Logger.getLogger("riftPlots").log(java.util.logging.Level.SEVERE,
                            pl.riftplots.config.Texts.raw("text.auction-service.nie-mozna-rozliczyc-aukcji", auction.getId()), e);
                }
            }
        }
    }

    private synchronized void finalizeAuction(PlotAuction auction) {
        if (journal == null) finalizeAuctionInternal(auction);
        else journal.execute("AUCTION", "auction:" + auction.getId(), auction.getPlotId(),
                auction.getCurrentBidderUuid(), auction.getCurrentBid(), () -> {
                    finalizeAuctionInternal(auction);
                    return auction.getStatus() != AuctionStatus.ACTIVE;
                });
        if (refunds != null) refunds.retryPending();
    }

    private void finalizeAuctionInternal(PlotAuction auction) {
        if (auction.getStatus() != AuctionStatus.ACTIVE || !auction.isExpired()) return;

        Optional<Plot> optPlot = plotManager.getPlot(auction.getPlotId());
        if (optPlot.isEmpty()) {
            cancelAuction(auction.getPlotId());
            return;
        }
        Plot plot = optPlot.get();
        if (!Objects.equals(plot.getOwnerUuid(), auction.getSellerUuid()) || plot.isRented()
                || (plot.getStatus() != PlotStatus.AVAILABLE && plot.getStatus() != PlotStatus.OWNED)) {
            cancelAuction(plot.getId());
            return;
        }

        if (auction.getCurrentBidderUuid() == null) {
            auction.setStatus(AuctionStatus.COMPLETED);
            saveAuction(auction);
            plot.setAuctionActive(false);
            plotManager.savePlot(plot);
            auditService.log(plot.getId(), null, pl.riftplots.config.Texts.raw("text.auction-service.system"), "AUCTION_CLOSED_NO_BIDS", pl.riftplots.config.Texts.raw("text.auction-service.brak-ofert-w-licytacji"));
            return;
        }

        org.bukkit.OfflinePlayer winner = org.bukkit.Bukkit.getOfflinePlayer(auction.getCurrentBidderUuid());
        BigDecimal winningBid = auction.getCurrentBid();

        if (plotManager.validateAcquisition(plot, winner.getUniqueId()).isValid()
                && (auction.isEscrowEnabled() || vaultHook.has(winner, winningBid) && vaultHook.withdraw(winner, winningBid))) {

            BigDecimal feePct = BigDecimal.valueOf(config.getAuctionCityFeePercent()).movePointLeft(2);
            if (auction.getSellerUuid() != null) {
                org.bukkit.OfflinePlayer seller = org.bukkit.Bukkit.getOfflinePlayer(auction.getSellerUuid());
                BigDecimal cityFee = winningBid.multiply(feePct).setScale(pl.riftplots.config.Settings.integer("economy.decimal-places"), pl.riftplots.config.Settings.rounding()).min(winningBid);
                BigDecimal sellerShare = winningBid.subtract(cityFee);
                if (!vaultHook.deposit(seller, sellerShare)) {
                    if (refunds != null && !auction.isEscrowEnabled()) {
                        refunds.prepare("auction-failed-payout:" + auction.getId(), "AUCTION_SETTLEMENT_REFUND", plot.getId(), journal.currentId(),
                                winner.getUniqueId(), winningBid, pl.riftplots.config.Texts.raw("text.auction-service.nieudana-wypata-dla-sprzedawcy-aukcji"));
                    } else if (!auction.isEscrowEnabled() && !vaultHook.deposit(winner, winningBid)) {
                        throw new IllegalStateException(pl.riftplots.config.Texts.raw("text.auction-service.nie-udao-sie-zwrocic-srodkow-zwyciezcy-aukcji", auction.getId()));
                    }
                    cancelAuction(plot.getId());
                    auditService.log(plot.getId(), winner.getUniqueId(), winner.getName(), "AUCTION_PAYMENT_FAILED", pl.riftplots.config.Texts.raw("text.auction-service.nieudana-wypata-dla-sprzedawcy-zwrocono-srodki-kupujacemu"));
                    return;
                }
                budgetService.recordTransaction(CityTransactionType.AUCTION_FEE, cityFee, plot.getId(), winner.getUniqueId(), pl.riftplots.config.Texts.raw("text.auction-service.prowizja-miejska-z-aukcji"));
            } else {
                budgetService.recordTransaction(CityTransactionType.PURCHASE_INCOME, winningBid, plot.getId(), winner.getUniqueId(), pl.riftplots.config.Texts.raw("text.auction-service.wygrana-w-licytacji-dziaki-miejskiej"));
            }

            plot.setOwnerUuid(winner.getUniqueId());
            plot.setOwnerName(winner.getName());
            plot.clearMembers();
            plot.clearHomeLocation();
            plot.setAuctionActive(false);
            plot.setResellAvailable(false);
            plot.setStatus(PlotStatus.OWNED);
            Instant now = Instant.now();
            plot.setPurchaseDate(now);
            if (plot.getNextTaxPayment() == null || !plot.getNextTaxPayment().isAfter(now)) {
                plot.setNextTaxPayment(now.plus(java.time.Duration.ofDays(plot.getBillingPeriodDays())));
                plot.setAdvancePeriodsPaid(0);
            }
            plot.setGracePeriodExpiry(null);
            plotManager.savePlot(plot);
            auction.setStatus(AuctionStatus.COMPLETED);
            saveAuction(auction);

            notificationService.notifyPlayer(winner.getUniqueId(), messageConfig.getRaw("auction.won")
                    .replace("{prefix}", config.getPrefix())
                    .replace("{name}", plot.getName())
                    .replace("{amount}", config.formatMoney(winningBid)), pl.riftplots.config.Settings.string("sounds.auction-won"));
            auditService.log(plot.getId(), winner.getUniqueId(), winner.getName(), "AUCTION_WON", "Kwota: " + winningBid);
        } else {
            cancelAuction(plot.getId());
            auditService.log(plot.getId(), winner.getUniqueId(), winner.getName(), "AUCTION_PAYMENT_FAILED", pl.riftplots.config.Texts.raw("text.auction-service.brak-srodkow-u-zwyciezcy"));
        }
    }

    public synchronized void cancelAuction(String plotId) {
        PlotAuction auction = activeAuctions.get(plotId.toLowerCase());
        if (auction != null && journal != null && !journal.isRunning()) {
            journal.execute("AUCTION_CANCEL", "auction-cancel:" + auction.getId(), plotId, auction.getCurrentBidderUuid(),
                    auction.getCurrentBid(), false, () -> {
                        cancelAuctionInternal(plotId);
                        return true;
                    });
        } else cancelAuctionInternal(plotId);
        if (refunds != null) refunds.retryPending();
    }

    private void cancelAuctionInternal(String plotId) {
        PlotAuction auction = activeAuctions.get(plotId.toLowerCase());
        if (auction != null) {
            if (auction.isEscrowEnabled() && auction.getCurrentBidderUuid() != null) {
                refunds.prepare("auction-cancel:" + auction.getId(), "AUCTION_CANCELLED", plotId, journal.currentId(),
                        auction.getCurrentBidderUuid(), auction.getCurrentBid(), pl.riftplots.config.Texts.raw("text.auction-service.anulowanie-aukcji-z-depozytem-ofert"));
            }
            auction.setStatus(AuctionStatus.CANCELLED);
            saveAuction(auction);
        }
        plotManager.getPlot(plotId).ifPresent(plot -> {
            plot.setAuctionActive(false);
            plotManager.savePlot(plot);
        });
    }

    public synchronized void cancelAuction(String plotId, String operator, String reason) {
        if (journal != null && journal.isBlocked(plotId))
            throw new IllegalArgumentException(pl.riftplots.config.Texts.raw("text.auction-service.najpierw-uzgodnij-trwajaca-operacje-finansowa"));
        cancelAuction(plotId);
        auditService.log(plotId, null, operator, "AUCTION_CANCELLED", reason);
    }

    public Collection<PlotAuction> getActiveAuctions() {
        return Collections.unmodifiableCollection(activeAuctions.values());
    }

    public Optional<PlotAuction> getAuction(String plotId) {
        if (plotId == null) return Optional.empty();
        return Optional.ofNullable(activeAuctions.get(plotId.toLowerCase()));
    }

    private List<AuctionBid> fileBids(YamlConfiguration yaml, String path, String id) {
        List<? extends Map<?, ?>> rows = yaml.getMapList(path + ".bids");
        if (rows.isEmpty()) {
            var migrated = YamlDataFile.load(new File(auctionsFile.getParentFile(), "auction_bids.yml"));
            var section = migrated.getConfigurationSection("bids");
            if (section != null)
                rows = section.getKeys(false).stream().filter(key -> id.equals(section.getString(key + ".auctionId")))
                        .sorted(Comparator.comparingLong(key -> section.getLong(key + ".sequence")))
                        .map(key -> section.getConfigurationSection(key).getValues(false)).toList();
        }
        List<AuctionBid> bids = new ArrayList<>();
        for (Map<?, ?> row : rows)
            bids.add(new AuctionBid(UUID.fromString(row.get("bidderUuid").toString()), row.get("bidderName").toString(),
                    new BigDecimal(row.get("amount").toString()), Instant.ofEpochMilli(Long.parseLong(row.get("timestamp").toString()))));
        return bids;
    }

    private List<AuctionBid> databaseBids(Connection connection, String id) throws SQLException {
        List<AuctionBid> bids = new ArrayList<>();
        try (var statement = connection.prepareStatement("SELECT * FROM rift_auction_bids WHERE auction_id = ? ORDER BY sequence")) {
            statement.setString(1, id);
            try (var result = statement.executeQuery()) {
                while (result.next())
                    bids.add(new AuctionBid(UUID.fromString(result.getString("bidder_uuid")), result.getString("bidder_name"),
                            result.getBigDecimal("amount"), Instant.ofEpochMilli(result.getLong("timestamp"))));
            }
        }
        return bids;
    }

    public synchronized List<AuctionBid> getBidHistory(String plotId, int page) {
        List<AuctionBid> bids;
        var active = getAuction(plotId);
        if (active.isPresent()) bids = new ArrayList<>(active.get().getBids());
        else if (databaseManager != null) {
            try (var connection = databaseManager.getConnection(); var statement = connection.prepareStatement("SELECT id FROM rift_auctions WHERE plot_id = ? ORDER BY start_time DESC LIMIT 1")) {
                statement.setString(1, plotId.toLowerCase());
                try (var result = statement.executeQuery()) {
                    bids = result.next() ? databaseBids(connection, result.getString(1)) : new ArrayList<>();
                }
            } catch (SQLException e) {
                throw new IllegalStateException(pl.riftplots.config.Texts.raw("text.auction-service.nie-mozna-odczytac-historii-aukcji"), e);
            }
        } else {
            var yaml = YamlDataFile.load(auctionsFile);
            var section = yaml.getConfigurationSection("auctions");
            var latest = section == null ? Optional.<String>empty() : section.getKeys(false).stream().filter(key -> plotId.equalsIgnoreCase(section.getString(key + ".plotId")))
                    .max(Comparator.comparingLong(key -> section.getLong(key + ".startTime")));
            bids = latest.isPresent() ? fileBids(yaml, "auctions." + latest.get(), latest.get()) : new ArrayList<>();
        }
        Collections.reverse(bids);
        return bids.stream().skip((long) Math.max(0, page) * pl.riftplots.config.Settings.integer("paging.auction-history")).limit(pl.riftplots.config.Settings.integer("paging.auction-history")).toList();
    }

    private void persistBids(Connection connection, PlotAuction auction) throws SQLException {
        try (var remove = connection.prepareStatement("DELETE FROM rift_auction_bids WHERE auction_id = ?")) {
            remove.setString(1, auction.getId());
            remove.executeUpdate();
        }
        try (var insert = connection.prepareStatement("INSERT INTO rift_auction_bids (auction_id, bidder_uuid, bidder_name, amount, timestamp, sequence) VALUES (?, ?, ?, ?, ?, ?)")) {
            int sequence = 0;
            for (var bid : auction.getBids()) {
                insert.setString(1, auction.getId());
                insert.setString(2, bid.getBidderUuid().toString());
                insert.setString(3, bid.getBidderName());
                insert.setBigDecimal(4, bid.getAmount());
                insert.setLong(5, bid.getTimestamp().toEpochMilli());
                insert.setInt(6, sequence++);
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }
}
