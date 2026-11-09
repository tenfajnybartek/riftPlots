package pl.riftplots.storage.sql;

import pl.riftplots.domain.player.PlotPlayerProfile;
import pl.riftplots.storage.repository.PlayerProfileRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

public class SqlPlayerProfileRepository implements PlayerProfileRepository {
    private final DatabaseManager databaseManager;

    public SqlPlayerProfileRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public List<PlotPlayerProfile> findAll() {
        List<PlotPlayerProfile> profiles = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM rift_player_profiles");
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                String owned = result.getString("owned_plots");
                Set<String> plotIds = owned == null || owned.isBlank() ? new HashSet<>()
                        : new HashSet<>(Arrays.asList(owned.split(",")));
                profiles.add(new PlotPlayerProfile(UUID.fromString(result.getString("uuid")), result.getString("last_name"),
                        Instant.ofEpochMilli(result.getLong("first_join")), Instant.ofEpochMilli(result.getLong("last_seen")), plotIds));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Nie udało się odczytać profili graczy", e);
        }
        return profiles;
    }

    @Override
    public Optional<PlotPlayerProfile> findById(UUID uuid) {
        if (uuid == null) {
            return Optional.empty();
        }
        String sql = "SELECT * FROM rift_player_profiles WHERE uuid = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String lastName = rs.getString("last_name");
                    long firstJoin = rs.getLong("first_join");
                    long lastSeen = rs.getLong("last_seen");
                    String ownedStr = rs.getString("owned_plots");
                    Set<String> owned = new HashSet<>();
                    if (ownedStr != null && !ownedStr.isBlank()) {
                        for (String id : ownedStr.split(",")) {
                            if (!id.isBlank()) {
                                owned.add(id.trim());
                            }
                        }
                    }
                    return Optional.of(new PlotPlayerProfile(uuid, lastName, Instant.ofEpochMilli(firstJoin), Instant.ofEpochMilli(lastSeen), owned));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Storage operation failed", e);
        }
        return Optional.empty();
    }

    @Override
    public void save(PlotPlayerProfile profile) {
        if (profile == null) {
            return;
        }
        String sqliteSql = """
            INSERT INTO rift_player_profiles (uuid, last_name, first_join, last_seen, owned_plots)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT(uuid) DO UPDATE SET
                last_name = excluded.last_name,
                last_seen = excluded.last_seen,
                owned_plots = excluded.owned_plots
        """;
        String mysqlSql = """
            INSERT INTO rift_player_profiles (uuid, last_name, first_join, last_seen, owned_plots)
            VALUES (?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                last_name = VALUES(last_name),
                last_seen = VALUES(last_seen),
                owned_plots = VALUES(owned_plots)
        """;

        boolean isSqlite = databaseManager.getStorageType() == pl.riftplots.storage.StorageType.SQLITE;
        String query = isSqlite ? sqliteSql : mysqlSql;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, profile.getUuid().toString());
            ps.setString(2, profile.getLastName());
            ps.setLong(3, profile.getFirstJoin().toEpochMilli());
            ps.setLong(4, profile.getLastSeen().toEpochMilli());
            ps.setString(5, String.join(",", profile.getOwnedPlotIds()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Storage operation failed", e);
        }
    }
}
