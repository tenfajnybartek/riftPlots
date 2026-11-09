package pl.riftplots.storage.mysql;

import pl.riftplots.config.StorageConfiguration;
import pl.riftplots.storage.StorageBackend;
import pl.riftplots.storage.StorageType;
import pl.riftplots.storage.repository.PlayerProfileRepository;
import pl.riftplots.storage.repository.PlotRepository;
import pl.riftplots.storage.repository.PlotSignRepository;
import pl.riftplots.storage.repository.TransactionRepository;
import pl.riftplots.storage.sql.*;

import java.io.File;

public class MysqlStorageBackend implements StorageBackend {
    private final DatabaseManager databaseManager;
    private SqlPlotRepository plotRepository;
    private SqlTransactionRepository transactionRepository;
    private SqlPlayerProfileRepository playerProfileRepository;
    private SqlPlotSignRepository plotSignRepository;

    public MysqlStorageBackend(StorageConfiguration storageConfig, File dataFolder) {
        this.databaseManager = new DatabaseManager(storageConfig, StorageType.MYSQL, dataFolder);
    }

    @Override
    public void initialize() throws Exception {
        databaseManager.initialize();
        this.plotRepository = new SqlPlotRepository(databaseManager);
        this.transactionRepository = new SqlTransactionRepository(databaseManager);
        this.playerProfileRepository = new SqlPlayerProfileRepository(databaseManager);
        this.plotSignRepository = new SqlPlotSignRepository(databaseManager);
    }

    @Override
    public void shutdown() {
        databaseManager.shutdown();
    }

    @Override
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    @Override
    public PlotRepository getPlotRepository() {
        return plotRepository;
    }

    @Override
    public TransactionRepository getTransactionRepository() {
        return transactionRepository;
    }

    @Override
    public PlayerProfileRepository getPlayerProfileRepository() {
        return playerProfileRepository;
    }

    @Override
    public PlotSignRepository getPlotSignRepository() {
        return plotSignRepository;
    }
}
