package pl.riftplots.storage.sql;

import pl.riftplots.domain.sign.PlotSign;
import pl.riftplots.domain.sign.PlotSignType;
import pl.riftplots.storage.repository.PlotSignRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class SqlPlotSignRepository implements PlotSignRepository {
    private final DatabaseManager databaseManager;

    public SqlPlotSignRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public List<PlotSign> findAll() {
        List<PlotSign> signs = new ArrayList<>();
        String sql = "SELECT * FROM rift_plot_signs";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                signs.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Storage operation failed", e);
        }
        return signs;
    }

    @Override
    public Optional<PlotSign> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        String sql = "SELECT * FROM rift_plot_signs WHERE id = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Storage operation failed", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<PlotSign> findByLocation(String worldName, int x, int y, int z) {
        if (worldName == null) {
            return Optional.empty();
        }
        String sql = "SELECT * FROM rift_plot_signs WHERE world = ? AND x = ? AND y = ? AND z = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, worldName);
            ps.setInt(2, x);
            ps.setInt(3, y);
            ps.setInt(4, z);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Storage operation failed", e);
        }
        return Optional.empty();
    }

    @Override
    public List<PlotSign> findByPlotId(String plotId) {
        if (plotId == null) {
            return Collections.emptyList();
        }
        List<PlotSign> signs = new ArrayList<>();
        String sql = "SELECT * FROM rift_plot_signs WHERE plot_id = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, plotId.toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    signs.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Storage operation failed", e);
        }
        return signs;
    }

    @Override
    public void save(PlotSign sign) {
        if (sign == null) {
            return;
        }
        String sqliteSql = """
            INSERT INTO rift_plot_signs (id, plot_id, type, world, x, y, z)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                plot_id = excluded.plot_id,
                type = excluded.type,
                world = excluded.world,
                x = excluded.x,
                y = excluded.y,
                z = excluded.z
        """;
        String mysqlSql = """
            INSERT INTO rift_plot_signs (id, plot_id, type, world, x, y, z)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                plot_id = VALUES(plot_id),
                type = VALUES(type),
                world = VALUES(world),
                x = VALUES(x),
                y = VALUES(y),
                z = VALUES(z)
        """;

        boolean isSqlite = databaseManager.getStorageType() == pl.riftplots.storage.StorageType.SQLITE;
        String query = isSqlite ? sqliteSql : mysqlSql;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, sign.getId());
            ps.setString(2, sign.getPlotId());
            ps.setString(3, sign.getType().name());
            ps.setString(4, sign.getWorldName());
            ps.setInt(5, sign.getX());
            ps.setInt(6, sign.getY());
            ps.setInt(7, sign.getZ());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Storage operation failed", e);
        }
    }

    @Override
    public void delete(String id) {
        if (id == null) {
            return;
        }
        String sql = "DELETE FROM rift_plot_signs WHERE id = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Storage operation failed", e);
        }
    }

    @Override
    public void deleteByPlotId(String plotId) {
        if (plotId == null) {
            return;
        }
        String sql = "DELETE FROM rift_plot_signs WHERE plot_id = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, plotId.toLowerCase());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Storage operation failed", e);
        }
    }

    private PlotSign mapRow(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String plotId = rs.getString("plot_id");
        PlotSignType type = PlotSignType.parse(rs.getString("type"));
        String world = rs.getString("world");
        int x = rs.getInt("x");
        int y = rs.getInt("y");
        int z = rs.getInt("z");
        return new PlotSign(id, plotId, type, world, x, y, z);
    }
}
