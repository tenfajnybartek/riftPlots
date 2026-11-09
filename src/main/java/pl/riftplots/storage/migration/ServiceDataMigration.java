package pl.riftplots.storage.migration;

import org.bukkit.configuration.file.YamlConfiguration;
import pl.riftplots.storage.StorageBackend;

import java.io.File;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.*;

/**
 * Copies the service tables that do not have a repository interface.
 */
final class ServiceDataMigration {
    private record Field(String column, String key, char type) {
    }

    private record Dataset(String table, String file, String root, List<Field> fields) {
    }

    private static Dataset dataset(String table, String file, String root, String... fields) {
        return new Dataset(table, file, root, Arrays.stream(fields).map(value -> {
            String[] parts = value.split(":");
            return new Field(parts[0], parts[1], parts.length == 3 ? parts[2].charAt(0) : 's');
        }).toList());
    }

    private static final List<Dataset> DATASETS = List.of(
            dataset("rift_rentals", "rentals.yml", "rentals", "plot_id:plotId", "owner_uuid:ownerUuid",
                    "tenant_uuid:tenantUuid", "tenant_name:tenantName", "rent_price:rentPrice:d", "period_unit:periodUnit",
                    "deposit:deposit:d", "start_date:startDate:l", "end_date:endDate:l", "auto_renew:autoRenew:b", "status:status"),
            dataset("rift_auctions", "auctions.yml", "auctions", "plot_id:plotId", "seller_uuid:sellerUuid",
                    "starting_price:startingPrice:d", "min_bid_increment:minBidIncrement:d", "current_bid:currentBid:d",
                    "current_bidder_uuid:currentBidderUuid", "current_bidder_name:currentBidderName", "start_time:startTime:l",
                    "end_time:endTime:l", "buyout_price:buyoutPrice:d", "status:status"),
            dataset("rift_city_budget", "city_budget.yml", "transactions", "type:type", "amount:amount:d",
                    "balance_after:balanceAfter:d", "timestamp:timestamp:l", "plot_id:plotId", "player_uuid:playerUuid", "description:description"),
            dataset("rift_audit_logs", "audit_logs.yml", "logs", "plot_id:plotId", "operator_uuid:operatorUuid",
                    "operator_name:operatorName", "action:action", "details:details", "timestamp:timestamp:l")
    );

    private final File dataFolder;

    ServiceDataMigration(File dataFolder) {
        this.dataFolder = dataFolder;
    }

    boolean isEmpty(StorageBackend backend) throws Exception {
        for (Dataset dataset : DATASETS) {
            if (!read(backend, dataset).getKeys(true).isEmpty()) return false;
        }
        return true;
    }

    void copy(StorageBackend source, StorageBackend target) throws Exception {
        for (Dataset dataset : DATASETS) {
            YamlConfiguration data = read(source, dataset);
            if (dataset.root().equals("transactions")) ensureBudgetBalance(data);
            write(target, dataset, data);
        }
    }

    private YamlConfiguration read(StorageBackend backend, Dataset dataset) throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        if (backend.getDatabaseManager() == null) {
            File file = new File(dataFolder, "data/" + dataset.file());
            if (file.exists()) yaml.load(file);
            return yaml;
        }
        try (Connection connection = backend.getDatabaseManager().getConnection();
             var statement = connection.prepareStatement("SELECT * FROM " + dataset.table());
             var result = statement.executeQuery()) {
            while (result.next()) {
                String path = dataset.root() + "." + result.getString("id");
                for (Field field : dataset.fields()) {
                    Object value = switch (field.type()) {
                        case 'l' -> result.getLong(field.column());
                        case 'b' -> result.getBoolean(field.column());
                        default -> result.getString(field.column());
                    };
                    if (!result.wasNull()) yaml.set(path + "." + field.key(), value);
                }
            }
        }
        return yaml;
    }

    private void ensureBudgetBalance(YamlConfiguration yaml) {
        var section = yaml.getConfigurationSection("transactions");
        if (section != null && !section.getKeys(false).isEmpty()) {
            String latest = section.getKeys(false).stream()
                    .max(Comparator.comparingLong(id -> section.getLong(id + ".timestamp"))).orElseThrow();
            yaml.set("balance", section.getString(latest + ".balanceAfter"));
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(new File(dataFolder, "config.yml"));
        String balance = yaml.getString("balance", String.valueOf(config.getDouble("city-budget.initial-balance", 100000)));
        yaml.set("balance", balance);
        String path = "transactions." + UUID.randomUUID();
        yaml.set(path + ".type", "ADMIN_ADJUSTMENT");
        yaml.set(path + ".amount", "0");
        yaml.set(path + ".balanceAfter", balance);
        yaml.set(path + ".timestamp", System.currentTimeMillis());
        yaml.set(path + ".description", "Saldo przeniesione podczas migracji");
    }

    private void write(StorageBackend backend, Dataset dataset, YamlConfiguration yaml) throws Exception {
        if (backend.getDatabaseManager() == null) {
            File file = new File(dataFolder, "data/" + dataset.file());
            file.getParentFile().mkdirs();
            yaml.save(file);
            return;
        }
        var section = yaml.getConfigurationSection(dataset.root());
        if (section == null) return;
        String columns = "id," + String.join(",", dataset.fields().stream().map(Field::column).toList());
        String parameters = String.join(",", Collections.nCopies(dataset.fields().size() + 1, "?"));
        try (Connection connection = backend.getDatabaseManager().getConnection()) {
            connection.setAutoCommit(false);
            try (var statement = connection.prepareStatement("INSERT INTO " + dataset.table() + " (" + columns + ") VALUES (" + parameters + ")")) {
                for (String id : section.getKeys(false)) {
                    statement.setString(1, id);
                    int index = 2;
                    for (Field field : dataset.fields()) {
                        String path = id + "." + field.key();
                        Object value = !section.contains(path) ? null : switch (field.type()) {
                            case 'l' -> section.getLong(path);
                            case 'b' -> section.getBoolean(path);
                            case 'd' -> new BigDecimal(section.getString(path));
                            default -> section.getString(path);
                        };
                        statement.setObject(index++, value);
                    }
                    statement.addBatch();
                }
                statement.executeBatch();
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        }
    }
}
