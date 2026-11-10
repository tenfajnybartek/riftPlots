package pl.riftplots.storage.sql;

import pl.riftplots.domain.plot.*;
import pl.riftplots.storage.repository.PlotRepository;

import java.math.BigDecimal;
import java.sql.*;
import java.time.Instant;
import java.util.*;

public class SqlPlotRepository implements PlotRepository {
    private final DatabaseManager databaseManager;

    public SqlPlotRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public List<Plot> findAll() {
        List<Plot> plots = new ArrayList<>();
        String sql = "SELECT * FROM rift_plots";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Plot plot = mapRow(rs);
                loadMembers(conn, plot);
                plots.add(plot);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(pl.riftplots.config.Texts.raw("text.sql-plot-repository.storage-operation-failed"), e);
        }
        return plots;
    }

    @Override
    public Optional<Plot> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        String sql = "SELECT * FROM rift_plots WHERE id = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id.toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Plot plot = mapRow(rs);
                    loadMembers(conn, plot);
                    return Optional.of(plot);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException(pl.riftplots.config.Texts.raw("text.sql-plot-repository.storage-operation-failed"), e);
        }
        return Optional.empty();
    }

    @Override
    public List<Plot> findByOwner(UUID ownerUuid) {
        if (ownerUuid == null) {
            return Collections.emptyList();
        }
        List<Plot> plots = new ArrayList<>();
        String sql = "SELECT * FROM rift_plots WHERE owner_uuid = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ownerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Plot plot = mapRow(rs);
                    loadMembers(conn, plot);
                    plots.add(plot);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException(pl.riftplots.config.Texts.raw("text.sql-plot-repository.storage-operation-failed"), e);
        }
        return plots;
    }

    @Override
    public void save(Plot plot) {
        if (plot == null) {
            return;
        }
        String sql = """
                    INSERT INTO rift_plots (
                        id, name, description, category, status, world, min_x, min_y, min_z, max_x, max_y, max_z,
                        worldguard_region_id, price, base_tax, tax_mode, tax_multiplier, tax_enabled,
                        billing_period_days, grace_period_hours, owner_uuid, owner_name, purchase_date,
                        next_tax_payment, grace_period_expiry, advance_periods_paid, schematic_file,
                        auto_reset_on_expire, refund_percent_on_abandon, created_at, updated_at,
                        property_type, district_id, rental_available, rental_price, rental_period_unit,
                        rental_deposit, active_rental_id, home_world, home_x, home_y, home_z, home_yaw, home_pitch,
                        resell_price, resell_available, auction_active, settings_overrides
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                        name = VALUES(name), description = VALUES(description), category = VALUES(category),
                        status = VALUES(status), world = VALUES(world), min_x = VALUES(min_x), min_y = VALUES(min_y),
                        min_z = VALUES(min_z), max_x = VALUES(max_x), max_y = VALUES(max_y), max_z = VALUES(max_z),
                        worldguard_region_id = VALUES(worldguard_region_id), price = VALUES(price), base_tax = VALUES(base_tax),
                        tax_mode = VALUES(tax_mode), tax_multiplier = VALUES(tax_multiplier), tax_enabled = VALUES(tax_enabled),
                        billing_period_days = VALUES(billing_period_days), grace_period_hours = VALUES(grace_period_hours),
                        owner_uuid = VALUES(owner_uuid), owner_name = VALUES(owner_name), purchase_date = VALUES(purchase_date),
                        next_tax_payment = VALUES(next_tax_payment), grace_period_expiry = VALUES(grace_period_expiry),
                        advance_periods_paid = VALUES(advance_periods_paid), schematic_file = VALUES(schematic_file),
                        auto_reset_on_expire = VALUES(auto_reset_on_expire), refund_percent_on_abandon = VALUES(refund_percent_on_abandon),
                        updated_at = VALUES(updated_at),
                        property_type = VALUES(property_type), district_id = VALUES(district_id),
                        rental_available = VALUES(rental_available), rental_price = VALUES(rental_price),
                        rental_period_unit = VALUES(rental_period_unit), rental_deposit = VALUES(rental_deposit),
                        active_rental_id = VALUES(active_rental_id), home_world = VALUES(home_world),
                        home_x = VALUES(home_x), home_y = VALUES(home_y), home_z = VALUES(home_z),
                        home_yaw = VALUES(home_yaw), home_pitch = VALUES(home_pitch),
                        resell_price = VALUES(resell_price), resell_available = VALUES(resell_available),
                        auction_active = VALUES(auction_active), settings_overrides = VALUES(settings_overrides)
                """;

        String sqliteSql = """
                    INSERT INTO rift_plots (
                        id, name, description, category, status, world, min_x, min_y, min_z, max_x, max_y, max_z,
                        worldguard_region_id, price, base_tax, tax_mode, tax_multiplier, tax_enabled,
                        billing_period_days, grace_period_hours, owner_uuid, owner_name, purchase_date,
                        next_tax_payment, grace_period_expiry, advance_periods_paid, schematic_file,
                        auto_reset_on_expire, refund_percent_on_abandon, created_at, updated_at,
                        property_type, district_id, rental_available, rental_price, rental_period_unit,
                        rental_deposit, active_rental_id, home_world, home_x, home_y, home_z, home_yaw, home_pitch,
                        resell_price, resell_available, auction_active, settings_overrides
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT(id) DO UPDATE SET
                        name = excluded.name, description = excluded.description, category = excluded.category,
                        status = excluded.status, world = excluded.world, min_x = excluded.min_x, min_y = excluded.min_y,
                        min_z = excluded.min_z, max_x = excluded.max_x, max_y = excluded.max_y, max_z = excluded.max_z,
                        worldguard_region_id = excluded.worldguard_region_id, price = excluded.price, base_tax = excluded.base_tax,
                        tax_mode = excluded.tax_mode, tax_multiplier = excluded.tax_multiplier, tax_enabled = excluded.tax_enabled,
                        billing_period_days = excluded.billing_period_days, grace_period_hours = excluded.grace_period_hours,
                        owner_uuid = excluded.owner_uuid, owner_name = excluded.owner_name, purchase_date = excluded.purchase_date,
                        next_tax_payment = excluded.next_tax_payment, grace_period_expiry = excluded.grace_period_expiry,
                        advance_periods_paid = excluded.advance_periods_paid, schematic_file = excluded.schematic_file,
                        auto_reset_on_expire = excluded.auto_reset_on_expire, refund_percent_on_abandon = excluded.refund_percent_on_abandon,
                        updated_at = excluded.updated_at,
                        property_type = excluded.property_type, district_id = excluded.district_id,
                        rental_available = excluded.rental_available, rental_price = excluded.rental_price,
                        rental_period_unit = excluded.rental_period_unit, rental_deposit = excluded.rental_deposit,
                        active_rental_id = excluded.active_rental_id, home_world = excluded.home_world,
                        home_x = excluded.home_x, home_y = excluded.home_y, home_z = excluded.home_z,
                        home_yaw = excluded.home_yaw, home_pitch = excluded.home_pitch,
                        resell_price = excluded.resell_price, resell_available = excluded.resell_available,
                        auction_active = excluded.auction_active, settings_overrides = excluded.settings_overrides
                """;

        boolean isSqlite = databaseManager.getStorageType() == pl.riftplots.storage.StorageType.SQLITE;
        String query = isSqlite ? sqliteSql : sql;

        try (Connection conn = databaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setString(1, plot.getId());
                ps.setString(2, plot.getName());
                ps.setString(3, plot.getDescription());
                ps.setString(4, plot.getCategory().name());
                ps.setString(5, plot.getStatus().name());

                PlotBounds bounds = plot.getBounds();
                ps.setString(6, bounds.getWorldName());
                ps.setInt(7, bounds.getMinX());
                ps.setInt(8, bounds.getMinY());
                ps.setInt(9, bounds.getMinZ());
                ps.setInt(10, bounds.getMaxX());
                ps.setInt(11, bounds.getMaxY());
                ps.setInt(12, bounds.getMaxZ());

                ps.setString(13, plot.getWorldGuardRegionId());
                ps.setBigDecimal(14, plot.getPrice());
                ps.setBigDecimal(15, plot.getBaseTax());
                ps.setString(16, plot.getTaxMode().name());
                ps.setDouble(17, plot.getTaxMultiplier());
                ps.setBoolean(18, plot.isTaxEnabled());
                ps.setInt(19, plot.getBillingPeriodDays());
                ps.setInt(20, plot.getGracePeriodHours());

                ps.setString(21, plot.getOwnerUuid() != null ? plot.getOwnerUuid().toString() : null);
                ps.setString(22, plot.getOwnerName());
                ps.setObject(23, plot.getPurchaseDate() != null ? plot.getPurchaseDate().toEpochMilli() : null);
                ps.setObject(24, plot.getNextTaxPayment() != null ? plot.getNextTaxPayment().toEpochMilli() : null);
                ps.setObject(25, plot.getGracePeriodExpiry() != null ? plot.getGracePeriodExpiry().toEpochMilli() : null);
                ps.setInt(26, plot.getAdvancePeriodsPaid());
                ps.setString(27, plot.getSchematicFile());
                ps.setBoolean(28, plot.isAutoResetOnExpire());
                ps.setDouble(29, plot.getRefundPercentOnAbandon());
                ps.setLong(30, plot.getCreatedAt().toEpochMilli());
                ps.setLong(31, plot.getUpdatedAt().toEpochMilli());

                ps.setString(32, plot.getPropertyType());
                ps.setString(33, plot.getDistrictId());
                ps.setBoolean(34, plot.isRentalAvailable());
                ps.setBigDecimal(35, plot.getRentalPrice());
                ps.setString(36, plot.getRentalPeriodUnit());
                ps.setBigDecimal(37, plot.getRentalDeposit());
                ps.setString(38, plot.getActiveRentalId());
                ps.setString(39, plot.getHomeWorld());
                ps.setObject(40, plot.getHomeX());
                ps.setObject(41, plot.getHomeY());
                ps.setObject(42, plot.getHomeZ());
                ps.setObject(43, plot.getHomeYaw());
                ps.setObject(44, plot.getHomePitch());
                ps.setBigDecimal(45, plot.getResellPrice());
                ps.setBoolean(46, plot.isResellAvailable());
                ps.setBoolean(47, plot.isAuctionActive());
                ps.setString(48, String.join(",", plot.getSettingOverrides().stream().map(Enum::name).toList()));

                ps.executeUpdate();
            }

            try (PreparedStatement psDel = conn.prepareStatement("DELETE FROM rift_plot_members WHERE plot_id = ?")) {
                psDel.setString(1, plot.getId());
                psDel.executeUpdate();
            }

            String memberSql = "INSERT INTO rift_plot_members (plot_id, player_uuid, player_name, role, permissions, added_at) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement psMem = conn.prepareStatement(memberSql)) {
                for (PlotMember member : plot.getMembers().values()) {
                    psMem.setString(1, plot.getId());
                    psMem.setString(2, member.getPlayerUuid().toString());
                    psMem.setString(3, member.getPlayerName());
                    psMem.setString(4, member.getRole().name());
                    String permsStr = String.join(",", member.getPermissions().stream().map(Enum::name).toList());
                    psMem.setString(5, permsStr);
                    psMem.setLong(6, member.getAddedAt().toEpochMilli());
                    psMem.addBatch();
                }
                psMem.executeBatch();
            }

            conn.commit();
        } catch (SQLException e) {
            throw new IllegalStateException(pl.riftplots.config.Texts.raw("text.sql-plot-repository.storage-operation-failed"), e);
        }
    }

    @Override
    public void saveAll(Iterable<Plot> plots) {
        if (plots == null) {
            return;
        }
        for (Plot plot : plots) {
            save(plot);
        }
    }

    @Override
    public void delete(String id) {
        if (id == null) {
            return;
        }
        try (Connection conn = databaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psMem = conn.prepareStatement("DELETE FROM rift_plot_members WHERE plot_id = ?")) {
                psMem.setString(1, id.toLowerCase());
                psMem.executeUpdate();
            }
            try (PreparedStatement psPlot = conn.prepareStatement("DELETE FROM rift_plots WHERE id = ?")) {
                psPlot.setString(1, id.toLowerCase());
                psPlot.executeUpdate();
            }
            conn.commit();
        } catch (SQLException e) {
            throw new IllegalStateException(pl.riftplots.config.Texts.raw("text.sql-plot-repository.storage-operation-failed"), e);
        }
    }

    private void loadMembers(Connection conn, Plot plot) throws SQLException {
        String sql = "SELECT * FROM rift_plot_members WHERE plot_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, plot.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("player_uuid"));
                    String name = rs.getString("player_name");
                    PlotMemberRole role = PlotMemberRole.parse(rs.getString("role"));
                    String permsStr = rs.getString("permissions");
                    EnumSet<PlotPermission> perms = EnumSet.noneOf(PlotPermission.class);
                    if (permsStr != null && !permsStr.isBlank()) {
                        for (String p : permsStr.split(",")) {
                            PlotPermission perm = PlotPermission.parse(p.trim());
                            if (perm != null) {
                                perms.add(perm);
                            }
                        }
                    }
                    long addedAtMilli = rs.getLong("added_at");
                    plot.addMember(new PlotMember(uuid, name, role, perms, Instant.ofEpochMilli(addedAtMilli)));
                }
            }
        }
    }

    private Plot mapRow(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String name = rs.getString("name");
        String description = rs.getString("description");
        PlotCategory category = PlotCategory.parse(rs.getString("category"));
        PlotStatus status = PlotStatus.valueOf(rs.getString("status"));

        String world = rs.getString("world");
        int minX = rs.getInt("min_x");
        int minY = rs.getInt("min_y");
        int minZ = rs.getInt("min_z");
        int maxX = rs.getInt("max_x");
        int maxY = rs.getInt("max_y");
        int maxZ = rs.getInt("max_z");
        PlotBounds bounds = new PlotBounds(world, minX, minY, minZ, maxX, maxY, maxZ);

        String regionId = rs.getString("worldguard_region_id");
        BigDecimal price = rs.getBigDecimal("price");
        BigDecimal baseTax = rs.getBigDecimal("base_tax");
        TaxMode taxMode = TaxMode.parse(rs.getString("tax_mode"));
        double taxMultiplier = rs.getDouble("tax_multiplier");
        boolean taxEnabled = rs.getBoolean("tax_enabled");
        int billingPeriodDays = rs.getInt("billing_period_days");
        int gracePeriodHours = rs.getInt("grace_period_hours");

        String ownerUuidStr = rs.getString("owner_uuid");
        UUID ownerUuid = ownerUuidStr != null ? UUID.fromString(ownerUuidStr) : null;
        String ownerName = rs.getString("owner_name");

        long purchaseMilli = rs.getLong("purchase_date");
        Instant purchaseDate = !rs.wasNull() && purchaseMilli > 0 ? Instant.ofEpochMilli(purchaseMilli) : null;

        long nextTaxMilli = rs.getLong("next_tax_payment");
        Instant nextTaxPayment = !rs.wasNull() && nextTaxMilli > 0 ? Instant.ofEpochMilli(nextTaxMilli) : null;

        long graceMilli = rs.getLong("grace_period_expiry");
        Instant gracePeriodExpiry = !rs.wasNull() && graceMilli > 0 ? Instant.ofEpochMilli(graceMilli) : null;

        int advancePeriods = rs.getInt("advance_periods_paid");
        String schematicFile = rs.getString("schematic_file");
        boolean autoReset = rs.getBoolean("auto_reset_on_expire");
        double refundPercent = rs.getDouble("refund_percent_on_abandon");

        long createdMilli = rs.getLong("created_at");
        long updatedMilli = rs.getLong("updated_at");

        Plot plot = new Plot(
                id, name, description, category, status, bounds, regionId,
                price, baseTax, taxMode, taxMultiplier, taxEnabled, billingPeriodDays, gracePeriodHours,
                ownerUuid, ownerName, Collections.emptyList(), purchaseDate, nextTaxPayment, gracePeriodExpiry,
                advancePeriods, schematicFile, autoReset, refundPercent,
                Instant.ofEpochMilli(createdMilli), Instant.ofEpochMilli(updatedMilli)
        );

        plot.setPropertyType(rs.getString("property_type"));
        plot.setDistrictId(rs.getString("district_id"));
        plot.setRentalAvailable(rs.getBoolean("rental_available"));
        BigDecimal rentPrice = rs.getBigDecimal("rental_price");
        plot.setRentalPrice(rentPrice != null ? rentPrice : BigDecimal.ZERO);
        plot.setRentalPeriodUnit(rs.getString("rental_period_unit"));
        BigDecimal rentDeposit = rs.getBigDecimal("rental_deposit");
        plot.setRentalDeposit(rentDeposit != null ? rentDeposit : BigDecimal.ZERO);
        plot.setActiveRentalId(rs.getString("active_rental_id"));

        String homeWorld = rs.getString("home_world");
        double homeX = rs.getDouble("home_x");
        if (!rs.wasNull() && homeWorld != null) {
            double homeY = rs.getDouble("home_y");
            double homeZ = rs.getDouble("home_z");
            float homeYaw = rs.getFloat("home_yaw");
            float homePitch = rs.getFloat("home_pitch");
            plot.setHomeLocation(homeWorld, homeX, homeY, homeZ, homeYaw, homePitch);
        }

        BigDecimal resPrice = rs.getBigDecimal("resell_price");
        plot.setResellPrice(resPrice != null ? resPrice : BigDecimal.ZERO);
        plot.setResellAvailable(rs.getBoolean("resell_available"));
        plot.setAuctionActive(rs.getBoolean("auction_active"));
        String overrides = rs.getString("settings_overrides");
        if (overrides != null)
            plot.setSettingOverrides(overrides.isEmpty() ? java.util.Set.of() : java.util.Arrays.stream(overrides.split(",")).map(PropertySetting::valueOf).collect(java.util.stream.Collectors.toSet()));

        return plot;
    }
}
