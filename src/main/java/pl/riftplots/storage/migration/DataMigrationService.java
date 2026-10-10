package pl.riftplots.storage.migration;

import pl.riftplots.config.StorageConfiguration;
import pl.riftplots.domain.payment.TransactionRecord;
import pl.riftplots.domain.player.PlotPlayerProfile;
import pl.riftplots.domain.plot.Plot;
import pl.riftplots.domain.sign.PlotSign;
import pl.riftplots.storage.StorageBackend;
import pl.riftplots.storage.StorageType;
import pl.riftplots.storage.mysql.MysqlStorageBackend;
import pl.riftplots.storage.sqlite.SqliteStorageBackend;
import pl.riftplots.storage.yaml.YamlStorageBackend;

import java.io.File;
import java.util.List;

public class DataMigrationService {
    private final File dataFolder;
    private final StorageConfiguration storageConfig;

    public DataMigrationService(File dataFolder, StorageConfiguration storageConfig) {
        this.dataFolder = dataFolder;
        this.storageConfig = storageConfig;
    }

    public MigrationReport migrate(StorageType source, StorageType target) throws Exception {
        if (source == target) {
            throw new IllegalArgumentException("Źródłowy i docelowy typ bazy danych nie mogą być takie same!");
        }

        StorageBackend srcBackend = createBackend(source);
        StorageBackend dstBackend = createBackend(target);

        srcBackend.initialize();
        dstBackend.initialize();

        List<Plot> plots = srcBackend.getPlotRepository().findAll();
        List<TransactionRecord> transactions = srcBackend.getTransactionRepository().findAll(10000);
        List<PlotSign> signs = srcBackend.getPlotSignRepository().findAll();

        int plotsCount = 0;
        for (Plot p : plots) {
            dstBackend.getPlotRepository().save(p);
            plotsCount++;
        }

        int txCount = 0;
        for (TransactionRecord tx : transactions) {
            dstBackend.getTransactionRepository().save(tx);
            txCount++;
        }

        int signCount = 0;
        for (PlotSign sign : signs) {
            dstBackend.getPlotSignRepository().save(sign);
            signCount++;
        }

        srcBackend.shutdown();
        dstBackend.shutdown();

        return new MigrationReport(source, target, plotsCount, txCount, signCount, true, "Migracja zakonczona sukcesem");
    }

    private StorageBackend createBackend(StorageType type) {
        return switch (type) {
            case YAML -> new YamlStorageBackend(dataFolder);
            case SQLITE -> new SqliteStorageBackend(storageConfig, dataFolder);
            case MYSQL -> new MysqlStorageBackend(storageConfig, dataFolder);
        };
    }

    public record MigrationReport(StorageType source, StorageType target, int plotsMigrated, int transactionsMigrated, int signsMigrated, boolean success, String message) {
    }
}
