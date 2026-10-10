package pl.riftplots.storage.yaml;

import org.bukkit.configuration.file.YamlConfiguration;
import pl.riftplots.domain.payment.TransactionRecord;
import pl.riftplots.domain.payment.TransactionStatus;
import pl.riftplots.domain.payment.TransactionType;
import pl.riftplots.storage.repository.TransactionRepository;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public class YamlTransactionRepository implements TransactionRepository {
    private final File transactionsDirectory;

    public YamlTransactionRepository(File transactionsDirectory) {
        this.transactionsDirectory = transactionsDirectory;
        if (!transactionsDirectory.exists()) {
            transactionsDirectory.mkdirs();
        }
    }

    @Override
    public void save(TransactionRecord record) {
        if (record == null) {
            return;
        }
        File file = new File(transactionsDirectory, record.getId() + ".yml");
        YamlConfiguration config = new YamlConfiguration();
        config.set("id", record.getId());
        config.set("plotId", record.getPlotId());
        config.set("playerUuid", record.getPlayerUuid().toString());
        config.set("playerName", record.getPlayerName());
        config.set("type", record.getType().name());
        config.set("amount", record.getAmount().toPlainString());
        config.set("status", record.getStatus().name());
        config.set("timestamp", record.getTimestamp().toEpochMilli());
        config.set("details", record.getDetails());

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<TransactionRecord> findByPlotId(String plotId) {
        if (plotId == null) {
            return Collections.emptyList();
        }
        List<TransactionRecord> result = new ArrayList<>();
        for (TransactionRecord record : findAll(500)) {
            if (plotId.equalsIgnoreCase(record.getPlotId())) {
                result.add(record);
            }
        }
        return result;
    }

    @Override
    public List<TransactionRecord> findByPlayer(UUID playerUuid) {
        if (playerUuid == null) {
            return Collections.emptyList();
        }
        List<TransactionRecord> result = new ArrayList<>();
        for (TransactionRecord record : findAll(500)) {
            if (playerUuid.equals(record.getPlayerUuid())) {
                result.add(record);
            }
        }
        return result;
    }

    @Override
    public List<TransactionRecord> findAll(int limit) {
        List<TransactionRecord> list = new ArrayList<>();
        File[] files = transactionsDirectory.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return list;
        }
        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());

        int count = 0;
        for (File file : files) {
            if (count >= limit) {
                break;
            }
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                String id = config.getString("id");
                String plotId = config.getString("plotId");
                String uuidStr = config.getString("playerUuid");
                String playerName = config.getString("playerName", "Unknown");
                TransactionType type = TransactionType.parse(config.getString("type"));
                BigDecimal amount = new BigDecimal(config.getString("amount", "0"));
                TransactionStatus status = TransactionStatus.valueOf(config.getString("status", "SUCCESS"));
                long timestamp = config.getLong("timestamp", System.currentTimeMillis());
                String details = config.getString("details", "");

                if (id != null && plotId != null && uuidStr != null) {
                    list.add(new TransactionRecord(
                            id, plotId, UUID.fromString(uuidStr), playerName,
                            type, amount, status, Instant.ofEpochMilli(timestamp), details
                    ));
                    count++;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return list;
    }
}
