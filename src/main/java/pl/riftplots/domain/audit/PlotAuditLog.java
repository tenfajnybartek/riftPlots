package pl.riftplots.domain.audit;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class PlotAuditLog {
    private final String id;
    private final String plotId;
    private final UUID operatorUuid;
    private final String operatorName;
    private final String action;
    private final String details;
    private final Instant timestamp;

    public PlotAuditLog(String id, String plotId, UUID operatorUuid, String operatorName, String action, String details, Instant timestamp) {
        this.id = Objects.requireNonNull(id, pl.riftplots.config.Texts.raw("text.plot-audit-log.id-cannot-be-null"));
        this.plotId = Objects.requireNonNull(plotId, pl.riftplots.config.Texts.raw("text.plot-audit-log.plotid-cannot-be-null")).toLowerCase();
        this.operatorUuid = operatorUuid;
        this.operatorName = operatorName != null ? operatorName : pl.riftplots.config.Texts.raw("text.plot-audit-log.system");
        this.action = Objects.requireNonNull(action, pl.riftplots.config.Texts.raw("text.plot-audit-log.action-cannot-be-null"));
        this.details = details != null ? details : "";
        this.timestamp = timestamp != null ? timestamp : Instant.now();
    }

    public static PlotAuditLog create(String plotId, UUID operatorUuid, String operatorName, String action, String details) {
        return new PlotAuditLog(UUID.randomUUID().toString(), plotId, operatorUuid, operatorName, action, details, Instant.now());
    }

    public String getId() {
        return id;
    }

    public String getPlotId() {
        return plotId;
    }

    public UUID getOperatorUuid() {
        return operatorUuid;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public String getAction() {
        return action;
    }

    public String getDetails() {
        return details;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
