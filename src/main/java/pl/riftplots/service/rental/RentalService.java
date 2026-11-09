package pl.riftplots.service.rental;

import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
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
    }

    private void loadFromFile() {
        activeAgreements.clear();
        if (!rentalsFile.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(rentalsFile);
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
            if (agreement.isActive()) {
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
            e.printStackTrace();
        }
    }

    public synchronized void saveAgreement(RentalAgreement agreement) {
        if (agreement == null) return;
        if (agreement.isActive()) {
            activeAgreements.put(agreement.getPlotId().toLowerCase(), agreement);
        } else {
            activeAgreements.remove(agreement.getPlotId().toLowerCase());
        }

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
                e.printStackTrace();
            }
        } else {
            try {
                YamlConfiguration config = rentalsFile.exists() ? YamlConfiguration.loadConfiguration(rentalsFile) : new YamlConfiguration();
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
                config.save(rentalsFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized boolean rentPlot(Player tenant, Plot plot, int periods, boolean autoRenew) {
        if (tenant == null || plot == null || !plot.isRentalAvailable() || plot.isRented()) {
            return false;
        }

        PropertyType propType = typeManager.getType(plot.getPropertyType());
        if (!propType.isRentalAllowed()) {
            tenant.sendMessage(messageConfig.get("rental.type-disallowed"));
            return false;
        }

        if (plot.getDistrictId() != null) {
            var dist = districtManager.getDistrict(plot.getDistrictId());
            if (dist.isPresent() && !dist.get().isRentalEnabled()) {
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
            vaultHook.deposit(owner, totalRent);
        } else {
            budgetService.recordTransaction(CityTransactionType.RENTAL_FEE, totalRent, plot.getId(), tenant.getUniqueId(), "Czynsz za wynajem lokalu miejskiego");
        }

        RentalAgreement agreement = RentalAgreement.create(
                plot.getId(), plot.getOwnerUuid(), tenant.getUniqueId(), tenant.getName(),
                plot.getRentalPrice(), unit, plot.getRentalDeposit(), periods, autoRenew
        );
        saveAgreement(agreement);

        plot.setActiveRentalId(agreement.getId());
        PlotMember tenantMember = new PlotMember(tenant.getUniqueId(), tenant.getName(), PlotMemberRole.MEMBER,
                EnumSet.of(PlotPermission.BUILD, PlotPermission.DESTROY, PlotPermission.CONTAINERS, PlotPermission.DOORS, PlotPermission.INTERACT),
                Instant.now());
        plot.addMember(tenantMember);
        plotManager.savePlot(plot);

        String durationStr = periods + " " + unit.name().toLowerCase();
        notificationService.notifyPlayer(tenant.getUniqueId(), messageConfig.getRaw("rental.success", "{prefix}<green>Wynajeto dzialke {name}!</green>")
                .replace("{prefix}", config.getPrefix())
                .replace("{name}", plot.getName())
                .replace("{plot_id}", plot.getId())
                .replace("{duration}", durationStr), Sound.ENTITY_PLAYER_LEVELUP);
        return true;
    }

    public synchronized void terminateRental(String plotId, boolean refundDeposit, String reason) {
        RentalAgreement agreement = activeAgreements.get(plotId.toLowerCase());
        if (agreement == null) return;

        agreement.setStatus(RentalStatus.TERMINATED);
        saveAgreement(agreement);

        Optional<Plot> optPlot = plotManager.getPlot(plotId);
        optPlot.ifPresent(plot -> {
            plot.setActiveRentalId(null);
            plot.removeMember(agreement.getTenantUuid());
            plotManager.savePlot(plot);
        });

        if (refundDeposit && agreement.getDeposit().compareTo(BigDecimal.ZERO) > 0) {
            org.bukkit.OfflinePlayer tenant = org.bukkit.Bukkit.getOfflinePlayer(agreement.getTenantUuid());
            vaultHook.deposit(tenant, agreement.getDeposit());
        }

        notificationService.notifyPlayer(agreement.getTenantUuid(), messageConfig.getRaw("rental.terminated", "{prefix}<red>Wynajem dzialki {plot_id} zostal zakonczony ({reason}).</red>")
                .replace("{prefix}", config.getPrefix())
                .replace("{plot_id}", plotId)
                .replace("{reason}", reason), Sound.BLOCK_NOTE_BLOCK_BASS);
    }

    public void checkExpiredRentals() {
        Instant now = Instant.now();
        for (RentalAgreement agreement : activeAgreements.values()) {
            if (agreement.getEndDate().isBefore(now)) {
                if (agreement.isAutoRenew()) {
                    org.bukkit.OfflinePlayer tenant = org.bukkit.Bukkit.getOfflinePlayer(agreement.getTenantUuid());
                    if (vaultHook.has(tenant, agreement.getRentPrice())) {
                        vaultHook.withdraw(tenant, agreement.getRentPrice());
                        agreement.setEndDate(now.plus(agreement.getPeriodUnit().getDuration()));
                        saveAgreement(agreement);
                        notificationService.notifyPlayer(agreement.getTenantUuid(), messageConfig.getRaw("rental.auto-renewed", "{prefix}<green>Przedluzono wynajem dzialki {name}.</green>")
                                .replace("{prefix}", config.getPrefix())
                                .replace("{name}", agreement.getPlotId()), Sound.BLOCK_NOTE_BLOCK_PLING);
                        continue;
                    }
                }
                terminateRental(agreement.getPlotId(), true, "Koniec okresu umowy");
            }
        }
    }

    public Optional<RentalAgreement> getActiveRental(String plotId) {
        if (plotId == null) return Optional.empty();
        return Optional.ofNullable(activeAgreements.get(plotId.toLowerCase()));
    }
}
