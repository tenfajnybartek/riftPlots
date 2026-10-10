package pl.riftplots.service.city;

import org.bukkit.configuration.file.YamlConfiguration;
import pl.riftplots.domain.city.CityTransaction;
import pl.riftplots.domain.city.CityTransactionType;
import pl.riftplots.storage.sql.DatabaseManager;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

public class CityBudgetService {
    private final File budgetFile;
    private final DatabaseManager databaseManager;
    private BigDecimal currentBalance = BigDecimal.valueOf(100000);
    private final List<CityTransaction> transactions = new ArrayList<>();

    public CityBudgetService(File dataFolder, DatabaseManager databaseManager) {
        this.budgetFile = new File(dataFolder, "data/city_budget.yml");
        this.databaseManager = databaseManager;
    }

    public void initialize() {
        if (databaseManager != null) {
            loadFromDatabase();
        } else {
            loadFromFile();
        }
    }

    private void loadFromFile() {
        if (!budgetFile.exists()) {
            saveToFile();
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(budgetFile);
        this.currentBalance = new BigDecimal(config.getString("balance", "100000.0"));
    }

    private void saveToFile() {
        try {
            YamlConfiguration config = new YamlConfiguration();
            config.set("balance", currentBalance.toPlainString());
            budgetFile.getParentFile().mkdirs();
            config.save(budgetFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadFromDatabase() {
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT balance_after FROM rift_city_budget ORDER BY timestamp DESC LIMIT 1");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                this.currentBalance = rs.getBigDecimal("balance_after");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public synchronized void recordTransaction(CityTransactionType type, BigDecimal amount, String plotId, UUID playerUuid, String description) {
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }

        if (type == CityTransactionType.TAX_INCOME || type == CityTransactionType.PURCHASE_INCOME
                || type == CityTransactionType.RENTAL_FEE || type == CityTransactionType.AUCTION_FEE) {
            currentBalance = currentBalance.add(amount);
        } else if (type == CityTransactionType.REFUND_EXPENSE || type == CityTransactionType.MAINTENANCE_EXPENSE) {
            currentBalance = currentBalance.subtract(amount);
        } else {
            currentBalance = currentBalance.add(amount);
        }

        CityTransaction tx = CityTransaction.create(type, amount, currentBalance, plotId, playerUuid, description);
        transactions.add(tx);

        if (databaseManager != null) {
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement("INSERT INTO rift_city_budget (id, type, amount, balance_after, timestamp, plot_id, player_uuid, description) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                ps.setString(1, tx.getId());
                ps.setString(2, tx.getType().name());
                ps.setBigDecimal(3, tx.getAmount());
                ps.setBigDecimal(4, tx.getBalanceAfter());
                ps.setLong(5, tx.getTimestamp().toEpochMilli());
                ps.setString(6, tx.getPlotId());
                ps.setString(7, tx.getPlayerUuid() != null ? tx.getPlayerUuid().toString() : null);
                ps.setString(8, tx.getDescription());
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            saveToFile();
        }
    }

    public synchronized BigDecimal getBalance() {
        return currentBalance;
    }

    public synchronized void adjustBalance(BigDecimal amount, UUID operatorUuid, String reason) {
        recordTransaction(CityTransactionType.ADMIN_ADJUSTMENT, amount, null, operatorUuid, reason);
    }

    public synchronized List<CityTransaction> getRecentTransactions(int limit) {
        if (databaseManager != null) {
            List<CityTransaction> list = new ArrayList<>();
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT * FROM rift_city_budget ORDER BY timestamp DESC LIMIT ?")) {
                ps.setInt(1, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String id = rs.getString("id");
                        CityTransactionType type = CityTransactionType.valueOf(rs.getString("type"));
                        BigDecimal amount = rs.getBigDecimal("amount");
                        BigDecimal balanceAfter = rs.getBigDecimal("balance_after");
                        long ts = rs.getLong("timestamp");
                        String plotId = rs.getString("plot_id");
                        String uuidStr = rs.getString("player_uuid");
                        UUID pUuid = uuidStr != null ? UUID.fromString(uuidStr) : null;
                        String desc = rs.getString("description");
                        list.add(new CityTransaction(id, type, amount, balanceAfter, Instant.ofEpochMilli(ts), plotId, pUuid, desc));
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return list;
        }

        int size = transactions.size();
        int from = Math.max(0, size - limit);
        return new ArrayList<>(transactions.subList(from, size));
    }
}
