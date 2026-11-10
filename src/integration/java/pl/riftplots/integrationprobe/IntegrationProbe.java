package pl.riftplots.integrationprobe;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Only included in the separate probe JAR, never in the production plugin.
 */
public final class IntegrationProbe extends JavaPlugin {
    private final Map<UUID, Double> balances = new HashMap<>();
    private final File marker = new File("plugins/RiftPlotsProbe/report.yml");
    private ClassLoader targetLoader;
    private int checks;
    private boolean waitingReset;

    private record Client(Player player, AtomicReference<Inventory> inventory) {
    }

    public void onEnable() {
        var state = YamlConfiguration.loadConfiguration(marker);
        var saved = state.getConfigurationSection("balances");
        if (saved != null)
            for (String key : saved.getKeys(false)) balances.put(UUID.fromString(key), saved.getDouble(key));
        Economy economy = (Economy) Proxy.newProxyInstance(Economy.class.getClassLoader(), new Class<?>[]{Economy.class}, (proxy, method, args) -> {
            String name = method.getName();
            if (name.equals("getName") || name.equals("toString")) return "RiftPlots isolated test economy";
            if (name.equals("isEnabled")) return true;
            if (name.equals("fractionalDigits")) return 2;
            if (name.equals("currencyNamePlural") || name.equals("currencyNameSingular")) return "testcoin";
            if (name.equals("format")) return String.format(Locale.ROOT, "%.2f testcoin", args[0]);
            if (name.equals("getBanks")) return List.of();
            if (name.equals("hasBankSupport")) return false;
            if (name.equals("hasAccount") || name.equals("createPlayerAccount")) return true;
            if (name.equals("getBalance") || name.equals("has") || name.equals("withdrawPlayer") || name.equals("depositPlayer")) {
                UUID id = args[0] instanceof OfflinePlayer player ? player.getUniqueId() : uuid(args[0].toString());
                double balance = balances.getOrDefault(id, 100000d);
                if (name.equals("getBalance")) return balance;
                double amount = ((Number) args[args.length - 1]).doubleValue();
                if (name.equals("has")) return balance >= amount;
                boolean success = Double.isFinite(amount) && amount >= 0 && (name.equals("depositPlayer") || balance >= amount);
                if (success) {
                    balance += name.equals("depositPlayer") ? amount : -amount;
                    balances.put(id, balance);
                }
                return new EconomyResponse(amount, balance, success ? EconomyResponse.ResponseType.SUCCESS : EconomyResponse.ResponseType.FAILURE, success ? null : "Declined");
            }
            if (method.getReturnType() == EconomyResponse.class)
                return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "No test banks");
            return defaultValue(method.getReturnType());
        });
        getServer().getServicesManager().register(Economy.class, economy, this, ServicePriority.Highest);
        getServer().getScheduler().runTaskLater(this, this::verify, 40L);
    }

    private void verify() {
        var report = YamlConfiguration.loadConfiguration(marker);
        try {
            var plugin = getServer().getPluginManager().getPlugin("riftPlots");
            check(plugin != null && plugin.isEnabled(), "riftPlots enabled with real dependencies");
            targetLoader = plugin.getClass().getClassLoader();
            Object plots = call(plugin, "getPlotManager"), auctions = call(plugin, "getAuctionService");
            Client buyer = client("ProbeBuyer"), tenant = client("ProbeTenant"), stranger = client("ProbeStranger");
            if (report.getInt("phase", 0) == 0) {
                Object ground = plot("probe_ground", 64), upper = plot("probe_upper", 69);
                check((boolean) call(plots, "registerPlot", ground), "ground plot registered");
                check((boolean) call(plots, "registerPlot", upper), "upper plot registered without vertical collision");
                Object snapshots = field(plugin, "snapshotService");
                check((boolean) call(snapshots, "createSnapshot", ground), "real WorldEdit ground schematic");
                check((boolean) call(snapshots, "createSnapshot", upper), "real WorldEdit upper schematic");
                check((boolean) call(call(plugin, "getPaymentService"), "buyPlot", buyer.player(), ground), "journaled Vault purchase");
                check(uuid("ProbeBuyer").equals(call(ground, "getOwnerUuid")), "ownership assigned");
                Object region = call(call(plugin, "getWorldGuardHook"), "getRegion", getServer().getWorlds().getFirst().getName(), "riftplot_probe_ground");
                check((boolean) call(call(region, "getOwners"), "contains", uuid("ProbeBuyer")), "real WorldGuard owner synchronized");
                check(Math.abs(balances.get(uuid("ProbeBuyer")) - 99000) < .001, "purchase debited once");
                Object member = cls("domain.plot.PlotMember").getMethod("defaultMember", UUID.class, String.class).invoke(null, uuid("ProbeTenant"), "ProbeTenant");
                call(ground, "addMember", member);
                call(plots, "savePlot", ground);
                Object protection = field(plugin, "protectionService");
                Location location = new Location(getServer().getWorlds().getFirst(), 1.5, 65, 1.5);
                check((boolean) call(protection, "canInteract", buyer.player(), location, constant("domain.plot.PlotPermission", "BUILD")), "owner can build");
                check(!(boolean) call(protection, "canInteract", tenant.player(), location, constant("domain.plot.PlotPermission", "BUILD")), "ordinary member cannot build");
                check(!(boolean) call(protection, "canInteract", stranger.player(), location, constant("domain.plot.PlotPermission", "INTERACT")), "stranger cannot interact");
                call(upper, "setRentalAvailable", true);
                call(upper, "setRentalPrice", BigDecimal.valueOf(100));
                call(upper, "setRentalDeposit", BigDecimal.valueOf(50));
                call(plots, "savePlot", upper);
                Object rentals = call(plugin, "getRentalService");
                check((boolean) call(rentals, "rentPlot", tenant.player(), upper, 1, false), "real rental settlement");
                check((boolean) call(protection, "canInteract", tenant.player(), new Location(location.getWorld(), 1.5, 70, 1.5), constant("domain.plot.PlotPermission", "BUILD")), "tenant can build upstairs");
                check(!(boolean) call(protection, "canInteract", tenant.player(), location, constant("domain.plot.PlotPermission", "BUILD")), "tenant has no build rights downstairs");
                check((boolean) call(rentals, "terminateRental", "probe_upper", true, "Integration test"), "rental released");
                check(Math.abs(balances.get(uuid("ProbeTenant")) - 99900) < .001, "deposit refunded once");
                call(upper, "setRentalAvailable", false);
                call(plots, "savePlot", upper);
                check((boolean) call(auctions, "startAuction", upper, null, BigDecimal.TEN, BigDecimal.valueOf(100), 10, BigDecimal.valueOf(20)), "auction started");
                check((boolean) call(auctions, "placeBid", tenant.player(), "probe_upper", BigDecimal.valueOf(19)), "ordinary bid stored");
                check((boolean) call(auctions, "placeBid", tenant.player(), "probe_upper", BigDecimal.valueOf(20)), "buyout allowed below next normal increment");
                check(uuid("ProbeTenant").equals(call(upper, "getOwnerUuid")), "auction ownership assigned");
                Object menu = call(field(plugin, "listGUI"), "market");
                Object view = cls("gui.MarketView").getMethod("start", cls("gui.MarketView$Tab")).invoke(null, constant("gui.MarketView$Tab", "OWNED"));
                call(menu, "open", buyer.player(), view);
                Inventory inventory = buyer.inventory().get();
                check(inventory != null && inventory.getSize() == 54, "real inventory GUI rendered");
                check("probe_ground".equals(inventory.getItem(0).getItemMeta().getPersistentDataContainer().get(new NamespacedKey("riftplots", "plot"), PersistentDataType.STRING)), "GUI uses persistent plot ID");
                report.set("phase", 1);
            } else if (report.getInt("phase") == 1) {
                Object ground = ((Optional<?>) call(plots, "getPlot", "probe_ground")).orElseThrow();
                Object upper = ((Optional<?>) call(plots, "getPlot", "probe_upper")).orElseThrow();
                check(uuid("ProbeBuyer").equals(call(ground, "getOwnerUuid")), "ground owner survived full server restart");
                check(uuid("ProbeTenant").equals(call(upper, "getOwnerUuid")), "auction owner survived full server restart");
                check(((List<?>) call(auctions, "getBidHistory", "probe_upper", 0)).size() == 2, "full bid history survived restart");
                check(Math.abs(balances.get(uuid("ProbeTenant")) - 99880) < .001, "no repeated refund or auction debit after restart");
                Object notifications = field(plugin, "notificationService");
                call(notifications, "deliverOfflineMessages", tenant.player());
                check(((List<?>) call(call(notifications, "getStore"), "pending", uuid("ProbeTenant"))).isEmpty(), "durable notification dispatch receipts");
                World world = getServer().getWorlds().getFirst();
                world.getBlockAt(1, 65, 1).setType(Material.DIAMOND_BLOCK, false);
                waitingReset = true;
                java.util.function.Consumer<Boolean> callback = success -> {
                    try {
                        check(success, "real batched WorldEdit reset completed");
                        check(world.getBlockAt(1, 65, 1).getType() == Material.AIR, "schematic restored the changed block");
                        check(call(ground, "getOwnerUuid") == null, "reset released ownership");
                        report.set("phase", 2);
                        report.set("passed", true);
                        report.set("checks", checks);
                        getLogger().info("RIFTPLOTS_INTEGRATION_PASS phase=2 checks=" + checks);
                    } catch (Throwable error) {
                        report.set("passed", false);
                        report.set("error", error.toString());
                        error.printStackTrace();
                    } finally {
                        waitingReset = false;
                        finish(report);
                    }
                };
                check((boolean) call(call(plugin, "getResetService"), "startReset", ground, callback), "reset accepted after restart");
                getServer().getScheduler().runTaskLater(this, () -> {
                    if (waitingReset) {
                        waitingReset = false;
                        report.set("passed", false);
                        report.set("error", "Reset timeout");
                        finish(report);
                    }
                }, 200L);
            } else {
                Object ground = ((Optional<?>) call(plots, "getPlot", "probe_ground")).orElseThrow();
                Object upper = ((Optional<?>) call(plots, "getPlot", "probe_upper")).orElseThrow();
                check(call(ground, "getOwnerUuid") == null, "restored backup keeps released ownership");
                check(uuid("ProbeTenant").equals(call(upper, "getOwnerUuid")), "restored backup keeps auction owner");
                check(((List<?>) call(auctions, "getBidHistory", "probe_upper", 0)).size() == 2, "restored backup keeps bids");
                check(Math.abs(balances.get(uuid("ProbeBuyer")) - 99000) < .001 && Math.abs(balances.get(uuid("ProbeTenant")) - 99880) < .001, "restored backup keeps economy balances");
                check(getServer().getWorlds().getFirst().getBlockAt(1, 65, 1).getType() == Material.AIR, "restored backup keeps world blocks");
                check((boolean) call(auctions, "startAuction", ground, null, BigDecimal.TEN, BigDecimal.ONE, 60, BigDecimal.valueOf(100)), "journaled auction creation after restore");
                Object menu = call(field(plugin, "listGUI"), "market");
                call(menu, "auction", buyer.player(), ground);
                Inventory auctionInventory = buyer.inventory().get();
                Object activeAuction = ((Optional<?>) call(auctions, "getAuction", "probe_ground")).orElseThrow();
                check(call(activeAuction, "getId").equals(auctionInventory.getItem(15).getItemMeta().getPersistentDataContainer()
                        .get(new NamespacedKey("riftplots", "auction"), PersistentDataType.STRING)), "real auction GUI binds buyout to its auction ID");
                call(auctions, "cancelAuction", "probe_ground");
                check(!(boolean) call(ground, "isAuctionActive"), "journaled auction cancellation after restore");
                verifyEditableConfiguration(plugin, buyer, ground);
                report.set("phase", 3);
            }
            check(((List<?>) call(call(plugin, "getOperationJournal"), "pending")).isEmpty(), "no unresolved financial operation");
            report.set("passed", true);
            report.set("checks", checks);
            report.set("server", getServer().getVersion());
            if (!waitingReset)
                getLogger().info("RIFTPLOTS_INTEGRATION_PASS phase=" + report.getInt("phase") + " checks=" + checks);
        } catch (Throwable error) {
            report.set("passed", false);
            report.set("error", error.toString());
            error.printStackTrace();
            getLogger().severe("RIFTPLOTS_INTEGRATION_FAILED");
        } finally {
            if (!waitingReset) finish(report);
        }
    }

    private void verifyEditableConfiguration(Object plugin, Client buyer, Object plot) throws Exception {
        File folder = (File) call(plugin, "getDataFolder");
        var guiFile = new File(folder, "gui.yml").toPath();
        var langFile = new File(folder, "lang.yml").toPath();
        var configFile = new File(folder, "config.yml").toPath();
        String originalGui = java.nio.file.Files.readString(guiFile), originalLang = java.nio.file.Files.readString(langFile), originalConfig = java.nio.file.Files.readString(configFile);
        try {
            var gui = YamlConfiguration.loadConfiguration(guiFile.toFile());
            gui.set("menus.buy.buttons.confirm.slot", 5);
            gui.set("menus.buy.buttons.confirm.material", "DIAMOND");
            gui.set("menus.buy.buttons.confirm.name", "<gold>Custom {plot_id}</gold>");
            gui.set("menus.buy.buttons.confirm.amount", 2);
            gui.set("menus.buy.buttons.confirm.glow", true);
            gui.set("menus.buy.buttons.confirm.custom-model-data", 12345);
            gui.set("menus.buy.filler.enabled", true);
            gui.save(guiFile.toFile());
            var lang = YamlConfiguration.loadConfiguration(langFile.toFile());
            lang.set("general.only-player", "<green>Configured language</green>");
            lang.save(langFile.toFile());
            call(plugin, "reloadPlugin");
            call(field(plugin, "confirmBuyGUI"), "open", buyer.player(), plot);
            Inventory inventory = buyer.inventory().get();
            var confirm = inventory.getItem(5);
            var meta = confirm.getItemMeta();
            check(confirm.getType() == Material.DIAMOND && confirm.getAmount() == 2, "GUI material amount and slot reload on Paper");
            check("confirm".equals(meta.getPersistentDataContainer().get(new NamespacedKey("riftplots", "action"), PersistentDataType.STRING)), "relocated GUI retains its action");
            check("1000".equals(meta.getPersistentDataContainer().get(new NamespacedKey("riftplots", "quote"), PersistentDataType.STRING)), "relocated GUI retains current financial quote");
            check(meta.hasEnchantmentGlintOverride() && meta.getEnchantmentGlintOverride() && meta.getCustomModelData() == 12345, "GUI glow and model data reload on Paper");
            check(inventory.getItem(0).getType() == Material.GRAY_STAINED_GLASS_PANE, "GUI filler is configurable");
            check("Custom probe_ground".equals(net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(meta.displayName())), "GUI placeholder rendered in custom name");
            check("<green>Configured language</green>".equals(call(field(plugin, "messageConfig"), "getRaw", "general.only-player")), "language reload uses edited catalog");
            String prefix = (String) call(field(plugin, "pluginConfig"), "getPrefix");
            var config = YamlConfiguration.loadConfiguration(configFile.toFile());
            config.set("general.prefix", "rejected-prefix");
            config.save(configFile.toFile());
            lang.set("general.only-player", List.of("invalid-type"));
            lang.save(langFile.toFile());
            boolean rejected = false;
            try {
                call(plugin, "reloadPlugin");
            } catch (InvocationTargetException expected) {
                rejected = expected.getCause() instanceof IllegalArgumentException;
            }
            check(rejected && prefix.equals(call(field(plugin, "pluginConfig"), "getPrefix")), "bad language reload preserves active plugin settings");
            check("confirm".equals(call(field(plugin, "guiConfig"), "actionAt", "buy", 5)), "bad reload preserves active GUI");
        } finally {
            java.nio.file.Files.writeString(guiFile, originalGui);
            java.nio.file.Files.writeString(langFile, originalLang);
            java.nio.file.Files.writeString(configFile, originalConfig);
            call(plugin, "reloadPlugin");
        }
    }

    private void finish(YamlConfiguration report) {
        try {
            getDataFolder().mkdirs();
            report.save(marker);
        } catch (Exception e) {
            e.printStackTrace();
        }
        getServer().getScheduler().runTaskLater(this, getServer()::shutdown, 5L);
    }

    public void onDisable() {
        var state = YamlConfiguration.loadConfiguration(marker);
        balances.forEach((id, balance) -> state.set("balances." + id, balance));
        try {
            getDataFolder().mkdirs();
            state.save(marker);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void check(boolean success, String message) {
        if (!success) throw new IllegalStateException(message);
        checks++;
    }

    private static UUID uuid(String name) {
        return UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
    }

    private Class<?> cls(String name) throws Exception {
        return Class.forName("pl.riftplots." + name, true, targetLoader);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Object constant(String name, String value) throws Exception {
        return Enum.valueOf((Class) cls(name), value);
    }

    private Object field(Object target, String name) throws Exception {
        var field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private Object call(Object target, String name, Object... arguments) throws Exception {
        for (var method : target.getClass().getMethods()) {
            if (!method.getName().equals(name) || method.getParameterCount() != arguments.length) continue;
            boolean matches = true;
            for (int i = 0; i < arguments.length; i++) {
                Class<?> type = method.getParameterTypes()[i];
                if (type == int.class) type = Integer.class;
                if (type == boolean.class) type = Boolean.class;
                if (type == double.class) type = Double.class;
                if (arguments[i] != null && !type.isInstance(arguments[i])) matches = false;
            }
            if (matches) return method.invoke(target, arguments);
        }
        throw new NoSuchMethodException(name);
    }

    private Object plot(String id, int y) throws Exception {
        World world = getServer().getWorlds().getFirst();
        for (int x = 0; x <= 4; x++)
            for (int z = 0; z <= 4; z++)
                for (int height = y; height <= y + 4; height++)
                    world.getBlockAt(x, height, z).setType(height == y ? Material.STONE : Material.AIR, false);
        Object bounds = cls("domain.plot.PlotBounds").getConstructors()[0].newInstance(world.getName(), 0, y, 0, 4, y + 4, 4);
        return cls("domain.plot.Plot").getConstructors()[0].newInstance(id, id, "Integration fixture", constant("domain.plot.PlotCategory", "RESIDENTIAL"),
                constant("domain.plot.PlotStatus", "AVAILABLE"), bounds, "riftplot_" + id, BigDecimal.valueOf(1000), BigDecimal.valueOf(100),
                constant("domain.plot.TaxMode", "FIXED"), 1d, true, 7, 6, null, null, List.of(), null, null, null, 0, id + ".schem", true, 50d, Instant.now(), Instant.now());
    }

    private Client client(String name) {
        var inventory = new AtomicReference<Inventory>();
        var location = new AtomicReference<>(new Location(getServer().getWorlds().getFirst(), 1.5, 65, 1.5));
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getUniqueId" -> uuid(name);
            case "getName", "toString" -> name;
            case "getWorld" -> location.get().getWorld();
            case "getLocation" -> location.get().clone();
            case "getServer" -> getServer();
            case "getPlayer" -> proxy;
            case "hasPermission" -> !args[0].toString().startsWith("riftplots.admin");
            case "isOnline", "isValid", "hasPlayedBefore" -> true;
            case "openInventory" -> {
                inventory.set((Inventory) args[0]);
                yield null;
            }
            case "teleport" -> {
                if (args[0] instanceof Location next) location.set(next);
                yield true;
            }
            case "hashCode" -> uuid(name).hashCode();
            case "equals" -> proxy == args[0];
            default -> defaultValue(method.getReturnType());
        });
        return new Client(player, inventory);
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == double.class) return 0d;
        if (type == float.class) return 0f;
        return null;
    }
}
