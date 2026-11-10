package pl.riftplots.storage.sql;

import pl.riftplots.domain.payment.TransactionRecord;
import pl.riftplots.domain.payment.TransactionStatus;
import pl.riftplots.domain.payment.TransactionType;
import pl.riftplots.storage.repository.TransactionRepository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class SqlTransactionRepository implements TransactionRepository {
    private final DatabaseManager databaseManager;

    public SqlTransactionRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public void save(TransactionRecord record) {
        if (record == null) {
            return;
        }
        String sql = "INSERT INTO rift_transactions (id, plot_id, player_uuid, player_name, type, amount, status, timestamp, details) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, record.getId());
            ps.setString(2, record.getPlotId());
            ps.setString(3, record.getPlayerUuid().toString());
            ps.setString(4, record.getPlayerName());
            ps.setString(5, record.getType().name());
            ps.setBigDecimal(6, record.getAmount());
            ps.setString(7, record.getStatus().name());
            ps.setLong(8, record.getTimestamp().toEpochMilli());
            ps.setString(9, record.getDetails());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Storage operation failed", e);
        }
    }

    @Override
    public List<TransactionRecord> findByPlotId(String plotId) {
        if (plotId == null) {
            return Collections.emptyList();
        }
        String sql = "SELECT * FROM rift_transactions WHERE plot_id = ? ORDER BY timestamp DESC LIMIT 100";
        return queryRecords(sql, plotId.toLowerCase());
    }

    @Override
    public List<TransactionRecord> findByPlayer(UUID playerUuid) {
        if (playerUuid == null) {
            return Collections.emptyList();
        }
        String sql = "SELECT * FROM rift_transactions WHERE player_uuid = ? ORDER BY timestamp DESC LIMIT 100";
        return queryRecords(sql, playerUuid.toString());
    }

    @Override
    public List<TransactionRecord> findAll(int limit) {
        String sql = "SELECT * FROM rift_transactions ORDER BY timestamp DESC LIMIT ?";
        List<TransactionRecord> list = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Storage operation failed", e);
        }
        return list;
    }

    private List<TransactionRecord> queryRecords(String sql, String param) {
        List<TransactionRecord> list = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Storage operation failed", e);
        }
        return list;
    }

    @Override
    public List<TransactionRecord> findByPlotId(String plotId, int page, int size) {
        return plotId == null ? List.of() : page("plot_id", plotId.toLowerCase(java.util.Locale.ROOT), page, size);
    }

    @Override
    public List<TransactionRecord> findByPlayer(UUID player, int page, int size) {
        return player == null ? List.of() : page("player_uuid", player.toString(), page, size);
    }

    @Override
    public List<TransactionRecord> findAll(int page, int size) {
        return page(null, null, page, size);
    }

    private List<TransactionRecord> page(String column, String value, int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("Invalid history page");
        String sql = "SELECT * FROM rift_transactions" + (column == null ? "" : " WHERE " + column + " = ?")
                + " ORDER BY timestamp DESC, id DESC LIMIT ? OFFSET ?";
        List<TransactionRecord> result = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection(); PreparedStatement query = connection.prepareStatement(sql)) {
            int parameter = 1;
            if (column != null) query.setString(parameter++, value);
            query.setInt(parameter++, size);
            query.setLong(parameter, (long) page * size);
            try (ResultSet rows = query.executeQuery()) {
                while (rows.next()) result.add(mapRow(rows));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot read history page", e);
        }
        return result;
    }

    private TransactionRecord mapRow(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String plotId = rs.getString("plot_id");
        UUID playerUuid = UUID.fromString(rs.getString("player_uuid"));
        String playerName = rs.getString("player_name");
        TransactionType type = TransactionType.parse(rs.getString("type"));
        BigDecimal amount = rs.getBigDecimal("amount");
        TransactionStatus status = TransactionStatus.valueOf(rs.getString("status"));
        long timestamp = rs.getLong("timestamp");
        String details = rs.getString("details");

        return new TransactionRecord(id, plotId, playerUuid, playerName, type, amount, status, Instant.ofEpochMilli(timestamp), details);
    }
}
