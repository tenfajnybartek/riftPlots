package pl.riftplots.storage.yaml;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import pl.riftplots.domain.plot.*;
import pl.riftplots.storage.repository.PlotRepository;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.*;

public class YamlPlotRepository implements PlotRepository {
    private final File plotsDirectory;

    public YamlPlotRepository(File plotsDirectory) {
        this.plotsDirectory = plotsDirectory;
        if (!plotsDirectory.exists()) {
            plotsDirectory.mkdirs();
        }
    }

    @Override
    public List<Plot> findAll() {
        List<Plot> list = new ArrayList<>();
        File[] files = plotsDirectory.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return list;
        }
        for (File file : files) {
            Plot plot = loadFromFile(file);
            if (plot != null) {
                list.add(plot);
            }
        }
        return list;
    }

    @Override
    public Optional<Plot> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        File file = new File(plotsDirectory, id.toLowerCase() + ".yml");
        if (!file.exists()) {
            return Optional.empty();
        }
        return Optional.ofNullable(loadFromFile(file));
    }

    @Override
    public List<Plot> findByOwner(UUID ownerUuid) {
        if (ownerUuid == null) {
            return Collections.emptyList();
        }
        List<Plot> result = new ArrayList<>();
        for (Plot plot : findAll()) {
            if (ownerUuid.equals(plot.getOwnerUuid())) {
                result.add(plot);
            }
        }
        return result;
    }

    @Override
    public void save(Plot plot) {
        if (plot == null) {
            return;
        }
        File file = new File(plotsDirectory, plot.getId().toLowerCase() + ".yml");
        File tempFile = new File(plotsDirectory, plot.getId().toLowerCase() + ".yml.tmp");

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", plot.getId());
        config.set("name", plot.getName());
        config.set("description", plot.getDescription());
        config.set("category", plot.getCategory().name());
        config.set("status", plot.getStatus().name());

        PlotBounds bounds = plot.getBounds();
        config.set("bounds.world", bounds.getWorldName());
        config.set("bounds.minX", bounds.getMinX());
        config.set("bounds.minY", bounds.getMinY());
        config.set("bounds.minZ", bounds.getMinZ());
        config.set("bounds.maxX", bounds.getMaxX());
        config.set("bounds.maxY", bounds.getMaxY());
        config.set("bounds.maxZ", bounds.getMaxZ());

        config.set("worldguard.regionId", plot.getWorldGuardRegionId());

        config.set("economy.price", plot.getPrice().toPlainString());
        config.set("economy.baseTax", plot.getBaseTax().toPlainString());
        config.set("economy.taxMode", plot.getTaxMode().name());
        config.set("economy.taxMultiplier", plot.getTaxMultiplier());
        config.set("economy.taxEnabled", plot.isTaxEnabled());
        config.set("economy.billingPeriodDays", plot.getBillingPeriodDays());
        config.set("economy.gracePeriodHours", plot.getGracePeriodHours());

        config.set("ownership.ownerUuid", plot.getOwnerUuid() != null ? plot.getOwnerUuid().toString() : null);
        config.set("ownership.ownerName", plot.getOwnerName());
        config.set("ownership.purchaseDate", plot.getPurchaseDate() != null ? plot.getPurchaseDate().toEpochMilli() : null);
        config.set("ownership.nextTaxPayment", plot.getNextTaxPayment() != null ? plot.getNextTaxPayment().toEpochMilli() : null);
        config.set("ownership.gracePeriodExpiry", plot.getGracePeriodExpiry() != null ? plot.getGracePeriodExpiry().toEpochMilli() : null);
        config.set("ownership.advancePeriodsPaid", plot.getAdvancePeriodsPaid());

        List<Map<String, Object>> membersData = new ArrayList<>();
        for (PlotMember member : plot.getMembers().values()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("uuid", member.getPlayerUuid().toString());
            map.put("name", member.getPlayerName());
            map.put("role", member.getRole().name());
            List<String> perms = member.getPermissions().stream().map(Enum::name).toList();
            map.put("permissions", perms);
            map.put("addedAt", member.getAddedAt().toEpochMilli());
            membersData.add(map);
        }
        config.set("members", membersData);

        config.set("reset.schematicFile", plot.getSchematicFile());
        config.set("reset.autoResetOnExpire", plot.isAutoResetOnExpire());
        config.set("reset.refundPercentOnAbandon", plot.getRefundPercentOnAbandon());

        config.set("propertyType", plot.getPropertyType());
        config.set("districtId", plot.getDistrictId());
        config.set("rental.available", plot.isRentalAvailable());
        config.set("rental.price", plot.getRentalPrice().toPlainString());
        config.set("rental.periodUnit", plot.getRentalPeriodUnit());
        config.set("rental.deposit", plot.getRentalDeposit().toPlainString());
        config.set("rental.activeRentalId", plot.getActiveRentalId());

        if (plot.hasHomeLocation()) {
            config.set("home.world", plot.getHomeWorld());
            config.set("home.x", plot.getHomeX());
            config.set("home.y", plot.getHomeY());
            config.set("home.z", plot.getHomeZ());
            config.set("home.yaw", plot.getHomeYaw());
            config.set("home.pitch", plot.getHomePitch());
        } else {
            config.set("home", null);
        }

        config.set("market.resellPrice", plot.getResellPrice().toPlainString());
        config.set("market.resellAvailable", plot.isResellAvailable());
        config.set("market.auctionActive", plot.isAuctionActive());

        config.set("timestamps.createdAt", plot.getCreatedAt().toEpochMilli());
        config.set("timestamps.updatedAt", plot.getUpdatedAt().toEpochMilli());

        try {
            config.save(tempFile);
            Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            try {
                config.save(file);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
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
        File file = new File(plotsDirectory, id.toLowerCase() + ".yml");
        if (file.exists()) {
            file.delete();
        }
    }

    private Plot loadFromFile(File file) {
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            String id = config.getString("id");
            if (id == null) {
                return null;
            }
            String name = config.getString("name", id);
            String description = config.getString("description", "");
            PlotCategory category = PlotCategory.parse(config.getString("category", "RESIDENTIAL"));
            PlotStatus status = PlotStatus.valueOf(config.getString("status", "AVAILABLE"));

            String world = config.getString("bounds.world");
            int minX = config.getInt("bounds.minX");
            int minY = config.getInt("bounds.minY");
            int minZ = config.getInt("bounds.minZ");
            int maxX = config.getInt("bounds.maxX");
            int maxY = config.getInt("bounds.maxY");
            int maxZ = config.getInt("bounds.maxZ");
            PlotBounds bounds = new PlotBounds(world, minX, minY, minZ, maxX, maxY, maxZ);

            String regionId = config.getString("worldguard.regionId", "riftplot_" + id);

            BigDecimal price = new BigDecimal(config.getString("economy.price", "25000.0"));
            BigDecimal baseTax = new BigDecimal(config.getString("economy.baseTax", "1500.0"));
            TaxMode taxMode = TaxMode.parse(config.getString("economy.taxMode", "FIXED"));
            double taxMultiplier = config.getDouble("economy.taxMultiplier", 1.0);
            boolean taxEnabled = config.getBoolean("economy.taxEnabled", true);
            int billingPeriodDays = config.getInt("economy.billingPeriodDays", 7);
            int gracePeriodHours = config.getInt("economy.gracePeriodHours", 6);

            String ownerUuidStr = config.getString("ownership.ownerUuid");
            UUID ownerUuid = ownerUuidStr != null && !ownerUuidStr.isBlank() ? UUID.fromString(ownerUuidStr) : null;
            String ownerName = config.getString("ownership.ownerName");

            long purchaseMilli = config.getLong("ownership.purchaseDate", 0);
            Instant purchaseDate = purchaseMilli > 0 ? Instant.ofEpochMilli(purchaseMilli) : null;

            long nextTaxMilli = config.getLong("ownership.nextTaxPayment", 0);
            Instant nextTaxPayment = nextTaxMilli > 0 ? Instant.ofEpochMilli(nextTaxMilli) : null;

            long graceMilli = config.getLong("ownership.gracePeriodExpiry", 0);
            Instant gracePeriodExpiry = graceMilli > 0 ? Instant.ofEpochMilli(graceMilli) : null;

            int advancePeriodsPaid = config.getInt("ownership.advancePeriodsPaid", 0);

            List<PlotMember> members = new ArrayList<>();
            List<Map<?, ?>> membersList = config.getMapList("members");
            for (Map<?, ?> map : membersList) {
                String uuidStr = (String) map.get("uuid");
                String mName = (String) map.get("name");
                String roleStr = (String) map.get("role");
                PlotMemberRole role = PlotMemberRole.parse(roleStr);
                EnumSet<PlotPermission> perms = EnumSet.noneOf(PlotPermission.class);
                Object rawPerms = map.get("permissions");
                if (rawPerms instanceof List<?> listPerms) {
                    for (Object pObj : listPerms) {
                        PlotPermission p = PlotPermission.parse(String.valueOf(pObj));
                        if (p != null) {
                            perms.add(p);
                        }
                    }
                }
                long addedMilli = 0;
                Object addedObj = map.get("addedAt");
                if (addedObj instanceof Number n) {
                    addedMilli = n.longValue();
                }
                Instant addedAt = addedMilli > 0 ? Instant.ofEpochMilli(addedMilli) : Instant.now();
                members.add(new PlotMember(UUID.fromString(uuidStr), mName, role, perms, addedAt));
            }

            String schematicFile = config.getString("reset.schematicFile");
            boolean autoReset = config.getBoolean("reset.autoResetOnExpire", true);
            double refundPercent = config.getDouble("reset.refundPercentOnAbandon", 50.0);

            long createdMilli = config.getLong("timestamps.createdAt", System.currentTimeMillis());
            long updatedMilli = config.getLong("timestamps.updatedAt", System.currentTimeMillis());

            Plot loadedPlot = new Plot(
                    id, name, description, category, status, bounds, regionId,
                    price, baseTax, taxMode, taxMultiplier, taxEnabled, billingPeriodDays, gracePeriodHours,
                    ownerUuid, ownerName, members, purchaseDate, nextTaxPayment, gracePeriodExpiry,
                    advancePeriodsPaid, schematicFile, autoReset, refundPercent,
                    Instant.ofEpochMilli(createdMilli), Instant.ofEpochMilli(updatedMilli)
            );

            loadedPlot.setPropertyType(config.getString("propertyType", "RESIDENTIAL"));
            loadedPlot.setDistrictId(config.getString("districtId"));
            loadedPlot.setRentalAvailable(config.getBoolean("rental.available", false));
            loadedPlot.setRentalPrice(new BigDecimal(config.getString("rental.price", "0")));
            loadedPlot.setRentalPeriodUnit(config.getString("rental.periodUnit", "DAY"));
            loadedPlot.setRentalDeposit(new BigDecimal(config.getString("rental.deposit", "0")));
            loadedPlot.setActiveRentalId(config.getString("rental.activeRentalId"));

            if (config.contains("home.world") && config.contains("home.x")) {
                loadedPlot.setHomeLocation(
                        config.getString("home.world"),
                        config.getDouble("home.x"),
                        config.getDouble("home.y"),
                        config.getDouble("home.z"),
                        (float) config.getDouble("home.yaw", 0.0),
                        (float) config.getDouble("home.pitch", 0.0)
                );
            }

            loadedPlot.setResellPrice(new BigDecimal(config.getString("market.resellPrice", "0")));
            loadedPlot.setResellAvailable(config.getBoolean("market.resellAvailable", false));
            loadedPlot.setAuctionActive(config.getBoolean("market.auctionActive", false));

            return loadedPlot;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
