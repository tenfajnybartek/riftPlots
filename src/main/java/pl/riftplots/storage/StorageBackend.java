package pl.riftplots.storage;

import pl.riftplots.storage.repository.PlayerProfileRepository;
import pl.riftplots.storage.repository.PlotRepository;
import pl.riftplots.storage.repository.PlotSignRepository;
import pl.riftplots.storage.repository.TransactionRepository;

public interface StorageBackend {
    default pl.riftplots.storage.sql.DatabaseManager getDatabaseManager() {
        return null;
    }

    void initialize() throws Exception;

    void shutdown() throws Exception;

    PlotRepository getPlotRepository();

    TransactionRepository getTransactionRepository();

    PlayerProfileRepository getPlayerProfileRepository();

    PlotSignRepository getPlotSignRepository();
}
