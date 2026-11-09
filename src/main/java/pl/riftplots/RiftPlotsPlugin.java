package pl.riftplots;

import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import pl.riftplots.command.PlotCommand;
import pl.riftplots.command.PlotTabCompleter;
import pl.riftplots.command.admin.AdminCommandDispatcher;
import pl.riftplots.config.MessageConfiguration;
import pl.riftplots.config.PluginConfiguration;
import pl.riftplots.config.StorageConfiguration;
import pl.riftplots.gui.*;
import pl.riftplots.integration.vault.VaultHook;
import pl.riftplots.integration.worldedit.WorldEditHook;
import pl.riftplots.integration.worldguard.WorldGuardHook;
import pl.riftplots.listener.PlotPistonFluidListener;
import pl.riftplots.listener.PlotProtectionListener;
import pl.riftplots.listener.PlotSessionListener;
import pl.riftplots.listener.PlotSignListener;
import pl.riftplots.scheduler.PlotTaxScheduler;
import pl.riftplots.service.payment.PaymentService;
import pl.riftplots.service.payment.TaxCalculator;
import pl.riftplots.service.plot.PlotManager;
import pl.riftplots.service.protection.PlotProtectionService;
import pl.riftplots.service.reset.PlotResetService;
import pl.riftplots.service.reset.PlotSnapshotService;
import pl.riftplots.service.sign.PlotSignService;
import pl.riftplots.storage.StorageBackend;
import pl.riftplots.storage.StorageType;
import pl.riftplots.storage.mysql.MysqlStorageBackend;
import pl.riftplots.storage.sqlite.SqliteStorageBackend;
import pl.riftplots.storage.yaml.YamlStorageBackend;

import java.io.File;

public class RiftPlotsPlugin extends JavaPlugin {
    private static RiftPlotsPlugin instance;

    private PluginConfiguration pluginConfig;
    private StorageConfiguration storageConfig;
    private MessageConfiguration messageConfig;

    private VaultHook vaultHook;
    private WorldEditHook worldEditHook;
    private WorldGuardHook worldGuardHook;

    private StorageBackend storageBackend;
    private PlotManager plotManager;
    private TaxCalculator taxCalculator;
    private PlotSnapshotService snapshotService;
    private PlotResetService resetService;
    private PlotSignService signService;
    private PaymentService paymentService;
    private PlotProtectionService protectionService;

    private pl.riftplots.domain.type.PropertyTypeManager typeManager;
    private pl.riftplots.service.district.DistrictManager districtManager;
    private pl.riftplots.service.notification.NotificationService notificationService;
    private pl.riftplots.service.city.CityBudgetService budgetService;
    private pl.riftplots.service.audit.AuditService auditService;
    private pl.riftplots.service.rental.RentalService rentalService;
    private pl.riftplots.service.transfer.PlotTransferService transferService;
    private pl.riftplots.service.auction.AuctionService auctionService;
    private pl.riftplots.service.teleport.TeleportService teleportService;
    private pl.riftplots.storage.migration.DataMigrationService migrationService;

    private PlotTaxScheduler taxScheduler;

    private PlotListGUI listGUI;
    private PlotDetailsGUI detailsGUI;
    private PlotConfirmBuyGUI confirmBuyGUI;
    private PlotMembersGUI membersGUI;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfigs();
        loadConfigurations();

        if (!setupIntegrations()) {
            getLogger().severe("Nie udalo sie zainicjalizowac wymaganych integracji (WorldGuard / WorldEdit / Vault)! Wylaczanie pluginu...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (!setupStorage()) {
            getLogger().severe("Nie udalo sie zainicjalizowac magazynu danych! Wylaczanie pluginu...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        setupServices();
        setupGUI();
        setupCommands();
        setupListeners();

        resetService.resumePendingResets();

        taxScheduler = new PlotTaxScheduler(
                this, pluginConfig, messageConfig, plotManager,
                taxCalculator, resetService, signService,
                storageBackend.getPlayerProfileRepository()
        );
        taxScheduler.setRentalService(rentalService);
        taxScheduler.setAuctionService(auctionService);
        taxScheduler.start();

        signService.updateAllSigns();

        getLogger().info("Plugin riftPlots zostal pomyslnie wlaczony! Zaladowano dzialek: " + plotManager.getAllPlots().size());
    }

    @Override
    public void onDisable() {
        if (taxScheduler != null) {
            taxScheduler.stop();
        }

        if (storageBackend != null) {
            try {
                storageBackend.shutdown();
            } catch (Exception e) {
                getLogger().severe("Blad podczas zamykania magazynu danych: " + e.getMessage());
            }
        }

        instance = null;
        getLogger().info("Plugin riftPlots zostal wylaczony.");
    }

    public void reloadPlugin() {
        if (taxScheduler != null) {
            taxScheduler.stop();
        }

        loadConfigurations();

        if (taxScheduler != null) {
            taxScheduler.start();
        }

        if (signService != null) {
            signService.updateAllSigns();
        }

        getLogger().info("Konfiguracja pluginu riftPlots zostala przeladowana.");
    }

    private void saveDefaultConfigs() {
        saveDefaultConfig();

        File storageFile = new File(getDataFolder(), "storage.yml");
        if (!storageFile.exists()) {
            saveResource("storage.yml", false);
        }

        File langFile = new File(getDataFolder(), "lang.yml");
        if (!langFile.exists()) {
            saveResource("lang.yml", false);
        }

        File typesFile = new File(getDataFolder(), "plot-types.yml");
        if (!typesFile.exists()) {
            saveResource("plot-types.yml", false);
        }

        File distFile = new File(getDataFolder(), "districts.yml");
        if (!distFile.exists()) {
            saveResource("districts.yml", false);
        }
    }

    private void loadConfigurations() {
        reloadConfig();
        pluginConfig = new PluginConfiguration();
        pluginConfig.load(getConfig());

        File storageFile = new File(getDataFolder(), "storage.yml");
        YamlConfiguration storageYaml = YamlConfiguration.loadConfiguration(storageFile);
        storageConfig = new StorageConfiguration();
        storageConfig.load(storageYaml);

        File langFile = new File(getDataFolder(), "lang.yml");
        YamlConfiguration langYaml = YamlConfiguration.loadConfiguration(langFile);
        messageConfig = new MessageConfiguration(pluginConfig);
        messageConfig.load(langYaml);
    }

    private boolean setupIntegrations() {
        vaultHook = new VaultHook(this);
        boolean vaultOk = vaultHook.setup();
        if (!vaultOk) {
            getLogger().warning("Brak dostepnego providera ekonomii w Vault! Funkcje platnosci moga byc niedostepne.");
        }

        worldEditHook = new WorldEditHook(this);
        if (!worldEditHook.isAvailable()) {
            getLogger().severe("Brak wtyczki WorldEdit!");
            return false;
        }

        worldGuardHook = new WorldGuardHook(this);
        if (!worldGuardHook.isAvailable()) {
            getLogger().severe("Brak wtyczki WorldGuard!");
            return false;
        }

        return true;
    }

    private boolean setupStorage() {
        StorageType type = storageConfig.getStorageType();
        try {
            if (type == StorageType.YAML) {
                storageBackend = new YamlStorageBackend(getDataFolder());
            } else if (type == StorageType.SQLITE) {
                storageBackend = new SqliteStorageBackend(storageConfig, getDataFolder());
            } else {
                storageBackend = new MysqlStorageBackend(storageConfig, getDataFolder());
            }
            storageBackend.initialize();
            return true;
        } catch (Exception e) {
            getLogger().severe("Blad inicjalizacji bazy danych (" + type + "): " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void setupServices() {
        plotManager = new PlotManager(storageBackend.getPlotRepository(), worldGuardHook);
        plotManager.loadAll();

        typeManager = new pl.riftplots.domain.type.PropertyTypeManager(getDataFolder());
        typeManager.load();

        districtManager = new pl.riftplots.service.district.DistrictManager(getDataFolder(), typeManager);
        districtManager.load();

        taxCalculator = new TaxCalculator(districtManager);
        snapshotService = new PlotSnapshotService(getDataFolder(), worldEditHook);
        resetService = new PlotResetService(this, pluginConfig, plotManager, snapshotService, worldEditHook);

        signService = new PlotSignService(pluginConfig, storageBackend.getPlotSignRepository(), plotManager, taxCalculator);
        resetService.setSignService(signService);

        notificationService = new pl.riftplots.service.notification.NotificationService();

        pl.riftplots.storage.sql.DatabaseManager dbMgr = null;
        if (storageBackend instanceof pl.riftplots.storage.sqlite.SqliteStorageBackend sqlite) {
            try {
                var field = pl.riftplots.storage.sqlite.SqliteStorageBackend.class.getDeclaredField("databaseManager");
                field.setAccessible(true);
                dbMgr = (pl.riftplots.storage.sql.DatabaseManager) field.get(sqlite);
            } catch (Exception ignored) {}
        } else if (storageBackend instanceof pl.riftplots.storage.mysql.MysqlStorageBackend mysql) {
            try {
                var field = pl.riftplots.storage.mysql.MysqlStorageBackend.class.getDeclaredField("databaseManager");
                field.setAccessible(true);
                dbMgr = (pl.riftplots.storage.sql.DatabaseManager) field.get(mysql);
            } catch (Exception ignored) {}
        }

        budgetService = new pl.riftplots.service.city.CityBudgetService(getDataFolder(), dbMgr);
        budgetService.initialize();

        auditService = new pl.riftplots.service.audit.AuditService(getDataFolder(), dbMgr, plotManager);

        rentalService = new pl.riftplots.service.rental.RentalService(
                getDataFolder(), dbMgr, plotManager, vaultHook,
                typeManager, districtManager, notificationService, budgetService,
                pluginConfig, messageConfig
        );
        rentalService.initialize();

        transferService = new pl.riftplots.service.transfer.PlotTransferService(
                plotManager, vaultHook, notificationService, budgetService,
                auditService, storageBackend.getTransactionRepository(),
                pluginConfig, messageConfig
        );

        auctionService = new pl.riftplots.service.auction.AuctionService(
                getDataFolder(), dbMgr, plotManager, vaultHook,
                notificationService, budgetService, auditService,
                pluginConfig, messageConfig
        );
        auctionService.initialize();

        teleportService = new pl.riftplots.service.teleport.TeleportService(plotManager, vaultHook, pluginConfig, messageConfig);

        migrationService = new pl.riftplots.storage.migration.DataMigrationService(getDataFolder(), storageConfig);

        paymentService = new PaymentService(
                pluginConfig, messageConfig, vaultHook, plotManager,
                taxCalculator, storageBackend.getTransactionRepository(),
                storageBackend.getPlayerProfileRepository(), resetService
        );
        paymentService.setSignService(signService);
        paymentService.setBudgetService(budgetService);
        paymentService.setAuditService(auditService);
        signService.setPaymentService(paymentService);

        protectionService = new PlotProtectionService(plotManager);
    }

    private void setupGUI() {
        listGUI = new PlotListGUI(pluginConfig, plotManager, taxCalculator);
        detailsGUI = new PlotDetailsGUI(pluginConfig, taxCalculator, paymentService);
        confirmBuyGUI = new PlotConfirmBuyGUI(pluginConfig);
        membersGUI = new PlotMembersGUI();
    }

    private void setupCommands() {
        AdminCommandDispatcher adminDispatcher = new AdminCommandDispatcher(
                this, pluginConfig, messageConfig, plotManager,
                taxCalculator, resetService, snapshotService, signService,
                worldEditHook, storageBackend.getTransactionRepository(),
                storageBackend.getPlayerProfileRepository(),
                districtManager, typeManager, budgetService,
                migrationService, auctionService, rentalService
        );

        PlotCommand plotCommand = new PlotCommand(
                pluginConfig, messageConfig, plotManager, taxCalculator,
                paymentService, storageBackend.getTransactionRepository(),
                rentalService, transferService, auctionService, teleportService,
                listGUI, adminDispatcher
        );

        PluginCommand cmd = getCommand("plots");
        if (cmd != null) {
            cmd.setExecutor(plotCommand);
            cmd.setTabCompleter(new PlotTabCompleter(plotCommand));
        }
    }

    private void setupListeners() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new PlotProtectionListener(pluginConfig, messageConfig, protectionService), this);
        pm.registerEvents(new PlotPistonFluidListener(pluginConfig, plotManager), this);
        pm.registerEvents(new PlotSignListener(signService), this);
        pm.registerEvents(new PlotSessionListener(pluginConfig, messageConfig, plotManager, taxCalculator, storageBackend.getPlayerProfileRepository(), notificationService), this);
        pm.registerEvents(new PlotGUIListener(plotManager, paymentService, listGUI, detailsGUI, confirmBuyGUI, membersGUI), this);
    }

    public static RiftPlotsPlugin getInstance() {
        return instance;
    }

    public PluginConfiguration getPluginConfig() {
        return pluginConfig;
    }

    public StorageConfiguration getStorageConfig() {
        return storageConfig;
    }

    public MessageConfiguration getMessageConfig() {
        return messageConfig;
    }

    public PlotManager getPlotManager() {
        return plotManager;
    }

    public PaymentService getPaymentService() {
        return paymentService;
    }

    public PlotResetService getResetService() {
        return resetService;
    }

    public PlotSignService getSignService() {
        return signService;
    }

    public pl.riftplots.domain.type.PropertyTypeManager getTypeManager() {
        return typeManager;
    }

    public pl.riftplots.service.district.DistrictManager getDistrictManager() {
        return districtManager;
    }

    public pl.riftplots.service.city.CityBudgetService getBudgetService() {
        return budgetService;
    }

    public pl.riftplots.service.rental.RentalService getRentalService() {
        return rentalService;
    }

    public pl.riftplots.service.transfer.PlotTransferService getTransferService() {
        return transferService;
    }

    public pl.riftplots.service.auction.AuctionService getAuctionService() {
        return auctionService;
    }

    public pl.riftplots.service.teleport.TeleportService getTeleportService() {
        return teleportService;
    }

    public pl.riftplots.service.audit.AuditService getAuditService() {
        return auditService;
    }

    public pl.riftplots.storage.migration.DataMigrationService getMigrationService() {
        return migrationService;
    }
}
