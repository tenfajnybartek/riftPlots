package pl.riftplots.service.audit;

import org.bukkit.configuration.file.YamlConfiguration;
import pl.riftplots.storage.yaml.YamlDataFile;
import pl.riftplots.domain.audit.PlotAuditLog;
import pl.riftplots.domain.plot.Plot;
import pl.riftplots.domain.plot.PlotStatus;
import pl.riftplots.service.plot.PlotManager;
import pl.riftplots.storage.sql.DatabaseManager;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

public class AuditService {
    private final File auditFile;
    private final DatabaseManager databaseManager;
    private final PlotManager plotManager;
    private java.util.function.BooleanSupplier writeAllowed = () -> true;

    public void setWriteAllowed(java.util.function.BooleanSupplier allowed) {
        writeAllowed = allowed;
    }

    public AuditService(File dataFolder, DatabaseManager databaseManager, PlotManager plotManager) {
        this.auditFile = new File(dataFolder, "data/audit_logs.yml");
        this.databaseManager = databaseManager;
        this.plotManager = plotManager;
    }

    public synchronized void log(String plotId, UUID operatorUuid, String operatorName, String action, String details) {
        if (!writeAllowed.getAsBoolean())
            throw new IllegalStateException(pl.riftplots.config.Texts.raw("text.audit-service.trwa-migracja-danych"));
        PlotAuditLog entry = PlotAuditLog.create(plotId, operatorUuid, operatorName, action, details);

        if (databaseManager != null) {
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement("INSERT INTO rift_audit_logs (id, plot_id, operator_uuid, operator_name, action, details, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                ps.setString(1, entry.getId());
                ps.setString(2, entry.getPlotId());
                ps.setString(3, entry.getOperatorUuid() != null ? entry.getOperatorUuid().toString() : null);
                ps.setString(4, entry.getOperatorName());
                ps.setString(5, entry.getAction());
                ps.setString(6, entry.getDetails());
                ps.setLong(7, entry.getTimestamp().toEpochMilli());
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new IllegalStateException(pl.riftplots.config.Texts.raw("text.audit-service.storage-operation-failed"), e);
            }
        } else {
            saveToFile(entry);
        }
    }

    private synchronized void saveToFile(PlotAuditLog entry) {
        try {
            YamlConfiguration config = auditFile.exists() ? YamlDataFile.load(auditFile) : new YamlConfiguration();
            String path = "logs." + entry.getId();
            config.set(path + ".plotId", entry.getPlotId());
            config.set(path + ".operatorUuid", entry.getOperatorUuid() != null ? entry.getOperatorUuid().toString() : null);
            config.set(path + ".operatorName", entry.getOperatorName());
            config.set(path + ".action", entry.getAction());
            config.set(path + ".details", entry.getDetails());
            config.set(path + ".timestamp", entry.getTimestamp().toEpochMilli());
            auditFile.getParentFile().mkdirs();
            YamlDataFile.save(config, auditFile);
        } catch (IOException e) {
            throw new IllegalStateException(pl.riftplots.config.Texts.raw("text.audit-service.storage-operation-failed"), e);
        }
    }

    public synchronized List<PlotAuditLog> getLogsForPlot(String plotId, int limit) {
        return getLogsForPlot(plotId, 0, limit);
    }

    public synchronized List<PlotAuditLog> getLogsForPlot(String plotId, int page, int limit) {
        if (page < 0)
            throw new IllegalArgumentException(pl.riftplots.config.Texts.raw("text.audit-service.invalid-audit-page"));
        if (plotId == null || limit <= 0) return Collections.emptyList();
        if (databaseManager != null) {
            List<PlotAuditLog> list = new ArrayList<>();
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT * FROM rift_audit_logs WHERE plot_id = ? ORDER BY timestamp DESC, id DESC LIMIT ? OFFSET ?")) {
                ps.setString(1, plotId.toLowerCase());
                ps.setInt(2, limit);
                ps.setLong(3, (long) page * limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String id = rs.getString("id");
                        String pId = rs.getString("plot_id");
                        String uStr = rs.getString("operator_uuid");
                        UUID opUuid = uStr != null ? UUID.fromString(uStr) : null;
                        String opName = rs.getString("operator_name");
                        String action = rs.getString("action");
                        String details = rs.getString("details");
                        long ts = rs.getLong("timestamp");
                        list.add(new PlotAuditLog(id, pId, opUuid, opName, action, details, Instant.ofEpochMilli(ts)));
                    }
                }
            } catch (SQLException e) {
                throw new IllegalStateException(pl.riftplots.config.Texts.raw("text.audit-service.storage-operation-failed"), e);
            }
            return list;
        }

        YamlConfiguration yaml = YamlDataFile.load(auditFile);
        List<PlotAuditLog> fileLogs = new ArrayList<>();
        var logs = yaml.getConfigurationSection("logs");
        if (logs != null) {
            for (String id : logs.getKeys(false)) {
                String path = "logs." + id;
                String uuid = yaml.getString(path + ".operatorUuid");
                fileLogs.add(new PlotAuditLog(id, yaml.getString(path + ".plotId"),
                        uuid != null ? UUID.fromString(uuid) : null, yaml.getString(path + ".operatorName"),
                        yaml.getString(path + ".action"), yaml.getString(path + ".details"),
                        Instant.ofEpochMilli(yaml.getLong(path + ".timestamp"))));
            }
        }
        return fileLogs.stream()
                .filter(l -> l.getPlotId().equalsIgnoreCase(plotId))
                .sorted(Comparator.comparing(PlotAuditLog::getTimestamp).thenComparing(PlotAuditLog::getId).reversed())
                .skip((long) page * limit)
                .limit(limit)
                .toList();
    }

    public Map<String, Object> calculateServerStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        Collection<Plot> all = plotManager.getAllPlots();

        int total = all.size();
        long available = all.stream().filter(p -> p.getStatus() == PlotStatus.AVAILABLE).count();
        long owned = all.stream().filter(p -> p.getStatus() == PlotStatus.OWNED).count();
        long grace = all.stream().filter(p -> p.getStatus() == PlotStatus.GRACE_PERIOD || p.getStatus() == PlotStatus.PAYMENT_DUE).count();
        long resetting = all.stream().filter(p -> p.getStatus() == PlotStatus.RESETTING).count();
        long disabled = all.stream().filter(p -> p.getStatus() == PlotStatus.DISABLED).count();
        long rented = all.stream().filter(Plot::isRented).count();
        long totalArea = all.stream().mapToLong(p -> p.getBounds().getHorizontalArea()).sum();

        stats.put("total_plots", total);
        stats.put("available_plots", available);
        stats.put("owned_plots", owned);
        stats.put("grace_period_plots", grace);
        stats.put("resetting_plots", resetting);
        stats.put("disabled_plots", disabled);
        stats.put("rented_plots", rented);
        stats.put("total_area_m2", totalArea);

        return stats;
    }
}
