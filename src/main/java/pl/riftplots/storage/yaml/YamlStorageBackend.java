package pl.riftplots.storage.yaml;

import pl.riftplots.storage.StorageBackend;
import pl.riftplots.storage.repository.PlayerProfileRepository;
import pl.riftplots.storage.repository.PlotRepository;
import pl.riftplots.storage.repository.PlotSignRepository;
import pl.riftplots.storage.repository.TransactionRepository;

import java.io.File;

public class YamlStorageBackend implements StorageBackend {
    private final File dataFolder;
    private YamlPlotRepository plotRepository;
    private YamlTransactionRepository transactionRepository;
    private YamlPlayerProfileRepository playerProfileRepository;
    private YamlPlotSignRepository plotSignRepository;

    public YamlStorageBackend(File dataFolder) {
        this.dataFolder = dataFolder;
    }

    @Override
    public void initialize() {
        File dataDir = new File(dataFolder, "data");
        File plotsDir = new File(dataDir, "plots");
        File transactionsDir = new File(dataDir, "transactions");
        File playersDir = new File(dataDir, "players");
        File signsFile = new File(dataDir, "signs.yml");
        File schemaFile = new File(dataDir, "schema.yml");
        var schema = YamlDataFile.load(schemaFile);
        if (schema.getInt("version", 1) > 2)
            throw new IllegalStateException(pl.riftplots.config.Texts.raw("text.yaml-storage-backend.magazyn-pochodzi-z-nowszej-wersji-riftplots"));
        schema.set("version", 2);
        try {
            YamlDataFile.save(schema, schemaFile);
        } catch (java.io.IOException e) {
            throw new IllegalStateException(pl.riftplots.config.Texts.raw("text.yaml-storage-backend.cannot-save-storage-schema-version"), e);
        }

        if (!plotsDir.exists()) plotsDir.mkdirs();
        if (!transactionsDir.exists()) transactionsDir.mkdirs();
        if (!playersDir.exists()) playersDir.mkdirs();

        this.plotRepository = new YamlPlotRepository(plotsDir);
        this.transactionRepository = new YamlTransactionRepository(transactionsDir);
        this.playerProfileRepository = new YamlPlayerProfileRepository(playersDir);
        this.plotSignRepository = new YamlPlotSignRepository(signsFile);
    }

    @Override
    public void shutdown() {
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
