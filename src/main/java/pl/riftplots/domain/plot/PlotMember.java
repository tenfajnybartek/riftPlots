package pl.riftplots.domain.plot;

import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

public class PlotMember {
    private final UUID playerUuid;
    private final String playerName;
    private PlotMemberRole role;
    private final Set<PlotPermission> permissions;
    private final Instant addedAt;

    public PlotMember(UUID playerUuid, String playerName, PlotMemberRole role, Set<PlotPermission> permissions, Instant addedAt) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.role = role != null ? role : PlotMemberRole.MEMBER;
        this.permissions = permissions != null && !permissions.isEmpty()
                ? EnumSet.copyOf(permissions) : EnumSet.noneOf(PlotPermission.class);
        this.addedAt = addedAt != null ? addedAt : Instant.now();
    }

    public static PlotMember defaultMember(UUID playerUuid, String playerName) {
        Set<PlotPermission> defaultPerms = pl.riftplots.config.Settings.permissions("members.default-permissions");
        return new PlotMember(playerUuid, playerName, PlotMemberRole.MEMBER, defaultPerms, Instant.now());
    }

    public static PlotMember defaultManager(UUID playerUuid, String playerName) {
        EnumSet<PlotPermission> managerPerms = EnumSet.allOf(PlotPermission.class);
        return new PlotMember(playerUuid, playerName, PlotMemberRole.MANAGER, managerPerms, Instant.now());
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public PlotMemberRole getRole() {
        return role;
    }

    public void setRole(PlotMemberRole role) {
        this.role = role;
    }

    public Set<PlotPermission> getPermissions() {
        return Collections.unmodifiableSet(permissions);
    }

    public boolean hasPermission(PlotPermission permission) {
        if (role == PlotMemberRole.MANAGER) {
            return true;
        }
        return permissions.contains(permission);
    }

    public void grantPermission(PlotPermission permission) {
        if (permission != null) {
            permissions.add(permission);
        }
    }

    public void revokePermission(PlotPermission permission) {
        if (permission != null) {
            permissions.remove(permission);
        }
    }

    public Instant getAddedAt() {
        return addedAt;
    }
}
