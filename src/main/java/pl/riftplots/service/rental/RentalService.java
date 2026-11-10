package pl.riftplots.service.rental;

import org.bukkit.configuration.file.YamlConfiguration;
import pl.riftplots.storage.yaml.YamlDataFile;
import org.bukkit.entity.Player;
import pl.riftplots.domain.city.CityTransactionType;
import pl.riftplots.domain.plot.*;
import pl.riftplots.domain.rental.RentalAgreement;
import pl.riftplots.domain.rental.RentalPeriodUnit;
import pl.riftplots.domain.rental.RentalStatus;
import pl.riftplots.domain.type.PropertyType;
import pl.riftplots.domain.type.PropertyTypeManager;
import pl.riftplots.integration.vault.VaultHook;
import pl.riftplots.service.city.CityBudgetService;
import pl.riftplots.service.district.DistrictManager;
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

public class RentalService {
    private pl.riftplots.service.transaction.OperationJournal journal;

    public void setOperationJournal(pl.riftplots.service.transaction.OperationJournal journal) {
        this.journal = journal;
    }

    private pl.riftplots.service.transaction.RefundService refunds;

    public void setRefundService(pl.riftplots.service.transaction.RefundService refunds) {
        this.refunds = refunds;
    }

    private final Map<String, RentalAgreement> committed = new HashMap<>();
    private final File rentalsFile;
    private final DatabaseManager databaseManager;
    private final PlotManager plotManager;
    private final VaultHook vaultHook;
    private final PropertyTypeManager typeManager;
    private final DistrictManager districtManager;
    private final NotificationService notificationService;
    private final CityBudgetService budgetService;
    private final pl.riftplots.config.PluginConfiguration config;
    private final pl.riftplots.config.MessageConfiguration messageConfig;

    private final Map<String, RentalAgreement> activeAgreements = new ConcurrentHashMap<>();

    public RentalService(File dataFolder,
                         DatabaseManager databaseManager,
                         PlotManager plotManager,
                         VaultHook vaultHook,
                         PropertyTypeManager typeManager,
                         DistrictManager districtManager,
                         NotificationService notificationService,
                         CityBudgetService budgetService,
                         pl.riftplots.config.PluginConfiguration config,
                         pl.riftplots.config.MessageConfiguration messageConfig) {
        this.rentalsFile = new File(dataFolder, "data/rentals.yml");
        this.databaseManager = databaseManager;
        this.plotManager = plotManager;
        this.vaultHook = vaultHook;
        this.typeManager = typeManager;
        this.districtManager = districtManager;
        this.notificationService = notificationService;
        this.budgetService = budgetService;
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
        activeAgreements.values().forEach(agreement -> committed.put(agreement.getId(), agreement.copy()));
        for (Plot plot : plotManager.getAllPlots()) {
            if (plot.isRented() && !activeAgreements.containsKey(plot.getId())) {
                plot.setActiveRentalId(null);
                plot.getMembers().values().stream().filter(member -> member.getRole() == PlotMemberRole.TENANT)
                        .map(PlotMember::getPlayerUuid).toList().forEach(plot::removeMember);
                plotManager.savePlot(plot);
            }
        }
        for (RentalAgreement agreement : activeAgreements.values()) {
            plotManager.getPlot(agreement.getPlotId()).ifPresent(plot -> {
                if ((plot.getStatus() == PlotStatus.AVAILABLE || plot.getStatus().isOwned())
                        && Objects.equals(plot.getOwnerUuid(), agreement.getOwnerUuid())) {
                    plot.setActiveRentalId(agreement.getId());
                    plot.addMember(new PlotMember(agreement.getTenantUuid(), agreement.getTenantName(), PlotMemberRole.TENANT,
                            pl.riftplots.config.Settings.permissions("rental.default-tenant-permissions"), agreement.getStartDate()));
                    plotManager.savePlot(plot);
                }
            });
        }
    }

    private void loadFromFile() {
        activeAgreements.clear();
        if (!rentalsFile.exists()) {
            return;
        }
        YamlConfiguration config = YamlDataFile.load(rentalsFile);
        var sec = config.getConfigurationSection("rentals");
        if (sec == null) return;

        for (String key : sec.getKeys(false)) {
            String p = "rentals." + key;
            String plotId = config.getString(p + ".plotId");
            String oStr = config.getString(p + ".ownerUuid");
            UUID oUuid = oStr != null && !oStr.isBlank() ? UUID.fromString(oStr) : null;
            UUID tUuid = UUID.fromString(config.getString(p + ".tenantUuid"));
            String tName = config.getString(p + ".tenantName");
            BigDecimal price = new BigDecimal(config.getString(p + ".rentPrice", "0"));
            RentalPeriodUnit unit = RentalPeriodUnit.parse(config.getString(p + ".periodUnit", "DAY"));
            BigDecimal deposit = new BigDecimal(config.getString(p + ".deposit", "0"));
            long start = config.getLong(p + ".startDate");
            long end = config.getLong(p + ".endDate");
            boolean autoRenew = config.getBoolean(p + ".autoRenew", false);
            RentalStatus status = RentalStatus.valueOf(config.getString(p + ".status", "ACTIVE"));

            RentalAgreement agreement = new RentalAgreement(
                    key, plotId, oUuid, tUuid, tName, price, unit, deposit,
                    Instant.ofEpochMilli(start), Instant.ofEpochMilli(end), autoRenew, status
            );
            if (agreement.getStatus() == RentalStatus.ACTIVE) {
                activeAgreements.put(plotId.toLowerCase(), agreement);
            }
        }
    }

    private void loadFromDatabase() {
        activeAgreements.clear();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM rift_rentals WHERE status = 'ACTIVE'");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String id = rs.getString("id");
                String plotId = rs.getString("plot_id");
                String oStr = rs.getString("owner_uuid");
                UUID oUuid = oStr != null && !oStr.isBlank() ? UUID.fromString(oStr) : null;
                UUID tUuid = UUID.fromString(rs.getString("tenant_uuid"));
                String tName = rs.getString("tenant_name");
                BigDecimal price = rs.getBigDecimal("rent_price");
                RentalPeriodUnit unit = RentalPeriodUnit.parse(rs.getString("period_unit"));
                BigDecimal deposit = rs.getBigDecimal("deposit");
                long start = rs.getLong("start_date");
                long end = rs.getLong("end_date");
                boolean autoRenew = rs.getBoolean("auto_renew");
                RentalStatus status = RentalStatus.valueOf(rs.getString("status"));

                RentalAgreement agreement = new RentalAgreement(
                        id, plotId, oUuid, tUuid, tName, price, unit, deposit,
                        Instant.ofEpochMilli(start), Instant.ofEpochMilli(end), autoRenew, status
                );
                activeAgreements.put(plotId.toLowerCase(), agreement);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(pl.riftplots.config.Texts.raw("text.rental-service.storage-operation-failed"), e);
        }
    }

    public synchronized void saveAgreement(RentalAgreement agreement) {
        if (agreement == null) return;
        try {
            persistAgreement(agreement);
        } catch (RuntimeException e) {
            RentalAgreement previous = committed.get(agreement.getId());
            if (previous != null) agreement.restore(previous);
            throw e;
        }
        if (agreement.getStatus() == RentalStatus.ACTIVE) {
            activeAgreements.put(agreement.getPlotId().toLowerCase(), agreement);
        } else {
            activeAgreements.remove(agreement.getPlotId().toLowerCase());
        }
        committed.put(agreement.getId(), agreement.copy());
    }

    private void persistAgreement(RentalAgreement agreement) {
        if (journal != null && !journal.isWriteAllowed())
            throw new IllegalStateException(pl.riftplots.config.Texts.raw("text.rental-service.trwa-migracja-danych"));
        if (databaseManager != null) {
            String sql = """
                        INSERT INTO rift_rentals (id, plot_id, owner_uuid, tenant_uuid, tenant_name, rent_price, period_unit, deposit, start_date, end_date, auto_renew, status)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT(id) DO UPDATE SET end_date = excluded.end_date, auto_renew = excluded.auto_renew, status = excluded.status
                    """;
            if (databaseManager.getStorageType() == pl.riftplots.storage.StorageType.MYSQL) {
                sql = """
                            INSERT INTO rift_rentals (id, plot_id, owner_uuid, tenant_uuid, tenant_name, rent_price, period_unit, deposit, start_date, end_date, auto_renew, status)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            ON DUPLICATE KEY UPDATE end_date = VALUES(end_date), auto_renew = VALUES(auto_renew), status = VALUES(status)
                        """;
            }
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, agreement.getId());
                ps.setString(2, agreement.getPlotId());
                ps.setString(3, agreement.getOwnerUuid() != null ? agreement.getOwnerUuid().toString() : null);
                ps.setString(4, agreement.getTenantUuid().toString());
                ps.setString(5, agreement.getTenantName());
                ps.setBigDecimal(6, agreement.getRentPrice());
                ps.setString(7, agreement.getPeriodUnit().name());
                ps.setBigDecimal(8, agreement.getDeposit());
                ps.setLong(9, agreement.getStartDate().toEpochMilli());
                ps.setLong(10, agreement.getEndDate().toEpochMilli());
                ps.setBoolean(11, agreement.isAutoRenew());
                ps.setString(12, agreement.getStatus().name());
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new IllegalStateException(pl.riftplots.config.Texts.raw("text.rental-service.storage-operation-failed"), e);
            }
        } else {
            try {
                YamlConfiguration config = rentalsFile.exists() ? YamlDataFile.load(rentalsFile) : new YamlConfiguration();
                String p = "rentals." + agreement.getId();
                config.set(p + ".plotId", agreement.getPlotId());
                config.set(p + ".ownerUuid", agreement.getOwnerUuid() != null ? agreement.getOwnerUuid().toString() : null);
                config.set(p + ".tenantUuid", agreement.getTenantUuid().toString());
                config.set(p + ".tenantName", agreement.getTenantName());
                config.set(p + ".rentPrice", agreement.getRentPrice().toPlainString());
                config.set(p + ".periodUnit", agreement.getPeriodUnit().name());
                config.set(p + ".deposit", agreement.getDeposit().toPlainString());
                config.set(p + ".startDate", agreement.getStartDate().toEpochMilli());
                config.set(p + ".endDate", agreement.getEndDate().toEpochMilli());
                config.set(p + ".autoRenew", agreement.isAutoRenew());
                config.set(p + ".status", agreement.getStatus().name());
                rentalsFile.getParentFile().mkdirs();
                YamlDataFile.save(config, rentalsFile);
            } catch (IOException e) {
                throw new IllegalStateException(pl.riftplots.config.Texts.raw("text.rental-service.storage-operation-failed"), e);
            }
        }
    }

    public synchronized boolean rentPlot(Player tenant, Plot plot, int periods, boolean autoRenew) {
        if (tenant == null || plot == null) return false;
        return journal == null ? rentPlotInternal(tenant, plot, periods, autoRenew) : journal.execute("RENTAL",
                "rent:" + plot.getId() + ":" + tenant.getUniqueId() + ":" + plot.getUpdatedAt().toEpochMilli(),
                plot.getId(), tenant.getUniqueId(), plot.getRentalPrice().multiply(BigDecimal.valueOf(Math.max(1, periods))).add(plot.getRentalDeposit()),
                () -> rentPlotInternal(tenant, plot, periods, autoRenew));
    }

    private boolean rentPlotInternal(Player tenant, Plot plot, int periods, boolean autoRenew) {
        if (tenant == null || plot == null || !plot.isRentalAvailable() || plot.isRented()
                || activeAgreements.containsKey(plot.getId()) || periods <= 0 || periods > config.getMaxRentalPeriods() || plot.isAuctionActive() || plotManager.isResetPending(plot.getId())
                || (plot.getStatus() != PlotStatus.AVAILABLE && plot.getStatus() != PlotStatus.OWNED)
                || plot.isOwner(tenant.getUniqueId()) || plot.isMember(tenant.getUniqueId())) {
            return false;
        }

        PropertyType propType = typeManager.getType(plot.getPropertyType());
        if (!propType.isRentalAllowed()) {
            tenant.sendMessage(messageConfig.get("rental.type-disallowed"));
            return false;
        }

        if (plot.getDistrictId() != null) {
            var dist = districtManager.getDistrict(plot.getDistrictId());
            if (dist.isEmpty() || !dist.get().isRentalEnabled() || !dist.get().isTypeAllowed(plot.getPropertyType())) {
                tenant.sendMessage(messageConfig.get("rental.district-disallowed"));
                return false;
            }
        }

        RentalPeriodUnit unit = RentalPeriodUnit.parse(plot.getRentalPeriodUnit());
        BigDecimal totalRent = plot.getRentalPrice().multiply(BigDecimal.valueOf(Math.max(1, periods)));
        BigDecimal totalCost = totalRent.add(plot.getRentalDeposit());

        if (!vaultHook.has(tenant, totalCost)) {
            tenant.sendMessage(messageConfig.get("rental.cannot-afford", Map.of("cost", config.formatMoney(totalCost))));
            return false;
        }

        if (!vaultHook.withdraw(tenant, totalCost)) {
            return false;
        }

        if (plot.getOwnerUuid() != null) {
            org.bukkit.OfflinePlayer owner = org.bukkit.Bukkit.getOfflinePlayer(plot.getOwnerUuid());
            if (!vaultHook.deposit(owner, totalRent)) {
                if (!vaultHook.deposit(tenant, totalCost)) {
                    throw new IllegalStateException(pl.riftplots.config.Texts.raw("text.rental-service.nie-udao-sie-zwrocic-srodkow-za-wynajem", plot.getId()));
                }
                tenant.sendMessage(messageConfig.get("general.economy-error"));
                return false;
            }
        } else {
            budgetService.recordTransaction(CityTransactionType.RENTAL_FEE, totalRent, plot.getId(), tenant.getUniqueId(), pl.riftplots.config.Texts.raw("text.rental-service.czynsz-za-wynajem-lokalu-miejskiego"));
        }

        RentalAgreement agreement = RentalAgreement.create(
                plot.getId(), plot.getOwnerUuid(), tenant.getUniqueId(), tenant.getName(),
                plot.getRentalPrice(), unit, plot.getRentalDeposit(), periods, autoRenew
        );
        saveAgreement(agreement);

        plot.setActiveRentalId(agreement.getId());
        PlotMember tenantMember = new PlotMember(tenant.getUniqueId(), tenant.getName(), PlotMemberRole.TENANT,
                pl.riftplots.config.Settings.permissions("rental.default-tenant-permissions"),
                Instant.now());
        plot.addMember(tenantMember);
        plotManager.savePlot(plot);

        String durationStr = pl.riftplots.config.Texts.raw("format.rental-duration", periods, unit);
        notificationService.notifyPlayer(tenant.getUniqueId(), messageConfig.getRaw("rental.success")
                .replace("{prefix}", config.getPrefix())
                .replace("{name}", plot.getName())
                .replace("{plot_id}", plot.getId())
                .replace("{duration}", durationStr), pl.riftplots.config.Settings.string("sounds.rental-start"));
        return true;
    }

    public synchronized boolean terminateRental(String plotId, boolean refundDeposit, String reason) {
        RentalAgreement agreement = activeAgreements.get(plotId.toLowerCase());
        if (agreement == null) {
            clearStaleRental(plotId);
            return true;
        }

        if (refundDeposit && agreement.getDeposit().signum() > 0) {
            if (refunds != null) refunds.prepare("rental-deposit:" + agreement.getId(), "RENTAL_DEPOSIT", plotId,
                    agreement.getId(), agreement.getTenantUuid(), agreement.getDeposit(), reason);
            else {
                org.bukkit.OfflinePlayer tenant = org.bukkit.Bukkit.getOfflinePlayer(agreement.getTenantUuid());
                if (!vaultHook.deposit(tenant, agreement.getDeposit())) return false;
            }
        }

        agreement.setStatus(RentalStatus.TERMINATED);
        saveAgreement(agreement);

        Optional<Plot> optPlot = plotManager.getPlot(plotId);
        optPlot.ifPresent(plot -> {
            plot.setActiveRentalId(null);
            plot.removeMember(agreement.getTenantUuid());
            plotManager.savePlot(plot);
        });
        if (refunds != null) refunds.retryPending();

        notificationService.notifyPlayer(agreement.getTenantUuid(), messageConfig.getRaw("rental.terminated")
                .replace("{prefix}", config.getPrefix())
                .replace("{plot_id}", plotId)
                .replace("{reason}", reason), pl.riftplots.config.Settings.string("sounds.rental-end"));
        return true;
    }

    public synchronized void checkExpiredRentals() {
        Instant now = Instant.now();
        for (Plot plot : plotManager.getAllPlots())
            if (plot.isRented() && !activeAgreements.containsKey(plot.getId())) clearStaleRental(plot.getId());
        for (RentalAgreement agreement : new ArrayList<>(activeAgreements.values())) {
            if (agreement.getEndDate().isAfter(now)) continue;
            try {
                if (journal == null) checkAgreement(agreement, now);
                else journal.execute("RENTAL_RENEWAL", "renewal:" + agreement.getId() + ":" + agreement.getEndDate(),
                        agreement.getPlotId(), agreement.getTenantUuid(), agreement.getRentPrice(), () -> checkAgreement(agreement, now));
            } catch (RuntimeException e) {
                java.util.logging.Logger.getLogger("riftPlots").log(java.util.logging.Level.SEVERE,
                        pl.riftplots.config.Texts.raw("text.rental-service.nie-mozna-rozliczyc-umowy", agreement.getId()), e);
            }
        }
        if (refunds != null) refunds.retryPending();
    }

    private boolean checkAgreement(RentalAgreement agreement, Instant now) {
        Optional<Plot> plot = plotManager.getPlot(agreement.getPlotId());
        boolean canRenew = plot.isPresent() && plot.get().isRentalAvailable() && !plotManager.isResetPending(agreement.getPlotId())
                && (plot.get().getStatus() == PlotStatus.AVAILABLE || plot.get().getStatus() == PlotStatus.OWNED)
                && Objects.equals(plot.get().getOwnerUuid(), agreement.getOwnerUuid());
        if (agreement.isAutoRenew() && canRenew) {
            var tenant = org.bukkit.Bukkit.getOfflinePlayer(agreement.getTenantUuid());
            if (vaultHook.has(tenant, agreement.getRentPrice()) && vaultHook.withdraw(tenant, agreement.getRentPrice())) {
                if (agreement.getOwnerUuid() != null) {
                    var owner = org.bukkit.Bukkit.getOfflinePlayer(agreement.getOwnerUuid());
                    if (!vaultHook.deposit(owner, agreement.getRentPrice())) {
                        if (!vaultHook.deposit(tenant, agreement.getRentPrice()))
                            throw new IllegalStateException(pl.riftplots.config.Texts.raw("text.rental-service.nie-udao-sie-zwrocic-czynszu", agreement.getId()));
                        terminateRental(agreement.getPlotId(), true, pl.riftplots.config.Texts.raw("text.rental-service.nieudane-rozliczenie-czynszu"));
                        return false;
                    }
                } else budgetService.recordTransaction(CityTransactionType.RENTAL_FEE, agreement.getRentPrice(),
                        agreement.getPlotId(), agreement.getTenantUuid(), pl.riftplots.config.Texts.raw("text.rental-service.odnowienie-wynajmu-lokalu-miejskiego"));
                agreement.setEndDate(now.plus(agreement.getPeriodUnit().getDuration()));
                saveAgreement(agreement);
                notificationService.notifyPlayer(agreement.getTenantUuid(), messageConfig.getRaw("rental.auto-renewed")
                        .replace("{prefix}", config.getPrefix()).replace("{name}", agreement.getPlotId()), pl.riftplots.config.Settings.string("sounds.rental-renewal"));
                return true;
            }
        }
        terminateRental(agreement.getPlotId(), true, pl.riftplots.config.Texts.raw("text.rental-service.koniec-okresu-umowy"));
        return false;
    }

    public synchronized boolean setAutoRenew(Player tenant, String plotId, boolean enabled) {
        RentalAgreement agreement = activeAgreements.get(plotId.toLowerCase());
        if (agreement == null || !agreement.getTenantUuid().equals(tenant.getUniqueId()) || !agreement.isActive())
            return false;
        agreement.setAutoRenew(enabled);
        saveAgreement(agreement);
        return true;
    }

    public int getMaxRentalPeriods() {
        return config.getMaxRentalPeriods();
    }

    public synchronized boolean offerRental(Player owner, Plot plot, BigDecimal price, RentalPeriodUnit unit, BigDecimal deposit) {
        if (plot == null || !plot.isOwner(owner.getUniqueId()) || plot.getStatus() != PlotStatus.OWNED
                || plot.isRented() || plot.isAuctionActive() || plotManager.isResetPending(plot.getId())
                || (journal != null && journal.isBlocked(plot.getId())) || !typeManager.getType(plot.getPropertyType()).isRentalAllowed())
            return false;
        if (plot.getDistrictId() != null && districtManager.getDistrict(plot.getDistrictId())
                .filter(district -> district.isRentalEnabled() && district.isTypeAllowed(plot.getPropertyType())).isEmpty())
            return false;
        if (!pl.riftplots.util.Money.isSafe(price) || !pl.riftplots.util.Money.isSafe(deposit)) return false;
        plot.setRentalPrice(price);
        plot.setRentalDeposit(deposit);
        plot.setRentalPeriodUnit(unit.name());
        plot.setRentalAvailable(true);
        plotManager.savePlot(plot);
        return true;
    }

    public synchronized boolean cancelByTenant(Player tenant, String plotId) {
        RentalAgreement agreement = activeAgreements.get(plotId.toLowerCase());
        return agreement != null && agreement.getTenantUuid().equals(tenant.getUniqueId())
                && terminateRental(plotId, true, pl.riftplots.config.Texts.raw("text.rental-service.wypowiedzenie-przez-najemce-opacony-czynsz-nie-podlega"));
    }

    public Optional<RentalAgreement> getActiveRental(String plotId) {
        if (plotId == null) return Optional.empty();
        return Optional.ofNullable(activeAgreements.get(plotId.toLowerCase()));
    }

    private void clearStaleRental(String plotId) {
        plotManager.getPlot(plotId).filter(Plot::isRented).ifPresent(plot -> {
            plot.setActiveRentalId(null);
            plot.getMembers().values().stream().filter(member -> member.getRole() == PlotMemberRole.TENANT)
                    .map(PlotMember::getPlayerUuid).toList().forEach(plot::removeMember);
            plotManager.savePlot(plot);
        });
    }
}
