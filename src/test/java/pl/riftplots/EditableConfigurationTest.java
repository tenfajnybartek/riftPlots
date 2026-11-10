package pl.riftplots;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.*;
import org.junit.jupiter.api.*;
import pl.riftplots.config.*;
import pl.riftplots.domain.plot.*;
import pl.riftplots.domain.rental.RentalPeriodUnit;
import pl.riftplots.gui.*;
import pl.riftplots.service.payment.*;
import pl.riftplots.service.plot.PlotManager;
import pl.riftplots.util.*;

import java.math.BigDecimal;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.regex.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EditableConfigurationTest {
    private final GuiConfiguration previousGui = Presentation.gui();
    private final MessageConfiguration previousMessages = Texts.catalog();

    @AfterEach
    void restore() {
        Presentation.install(previousGui);
        Texts.install(previousMessages);
        Settings.install(TestFixtures.resource("config.yml"));
    }

    @Test
    void olderLanguageFileGetsNewDefaultsWithoutLosingOverrides() {
        var override = new YamlConfiguration();
        override.set("general.only-player", "<green>Moja wiadomość</green>");
        var messages = new MessageConfiguration(TestFixtures.config());
        messages.load(override);
        assertEquals("Moja wiadomość", PlainTextComponentSerializer.plainText().serialize(messages.get("general.only-player")));
        assertTrue(messages.contains("gui-messages.failure"));
        assertEquals("włączone", messages.getRaw("labels.enabled"));
        override.set("general.only-player", "");
        messages.load(override);
        assertEquals("", PlainTextComponentSerializer.plainText().serialize(messages.get("general.only-player")));
    }

    @Test
    void placeholdersCannotInjectMiniMessageOrExpandOtherPlaceholders() {
        var override = new YamlConfiguration();
        override.set("example.line", "<green>{arg1}</green> | {arg2}");
        var messages = new MessageConfiguration(TestFixtures.config());
        messages.load(override);
        Texts.install(messages);
        assertEquals("<red>&c {arg2}</red> | koniec", PlainTextComponentSerializer.plainText().serialize(
                Texts.component("example.line", "<red>&c {arg2}</red>", "koniec")));
    }

    @Test
    void invalidLanguageTypeKeepsThePreviousCatalog() {
        var messages = new MessageConfiguration(TestFixtures.config());
        String previous = messages.getRaw("general.only-player");
        var invalid = new YamlConfiguration();
        invalid.set("general.only-player", List.of("not a scalar"));
        assertThrows(IllegalArgumentException.class, () -> messages.load(invalid));
        assertEquals(previous, messages.getRaw("general.only-player"));
    }

    @Test
    void guiLayoutMovesActionsAndRejectsCollisionsWithoutReplacingTheLastGoodLayout() {
        var gui = new GuiConfiguration();
        var custom = new YamlConfiguration();
        custom.set("menus.buy.buttons.confirm.slot", 5);
        custom.set("menus.buy.buttons.confirm.material", "DIAMOND");
        custom.set("menus.market.default-sort", "PRICE");
        gui.load(custom);
        Presentation.install(gui);
        assertEquals(MarketView.Sort.PRICE, MarketView.start(MarketView.Tab.SALE).sort());
        assertEquals("confirm", gui.actionAt("buy", 5));
        assertNull(gui.actionAt("buy", 11));
        var invalid = new YamlConfiguration();
        invalid.set("menus.buy.buttons.confirm.slot", 15);
        assertThrows(IllegalArgumentException.class, () -> gui.load(invalid));
        assertEquals("confirm", gui.actionAt("buy", 5));
    }

    @Test
    void hiddenButtonsMayBeOutsideASmallerInventoryAndDoNotHaveAnAction() {
        var gui = new GuiConfiguration();
        var custom = new YamlConfiguration();
        custom.set("menus.buy.size", 9);
        custom.set("menus.buy.buttons.confirm.slot", 5);
        custom.set("menus.buy.buttons.cancel.slot", 8);
        custom.set("menus.buy.buttons.border.enabled", false);
        gui.load(custom);
        assertEquals(9, gui.size("buy"));
        assertFalse(gui.enabled("buy", "border"));
        assertNull(gui.actionAt("buy", 22));
    }

    @Test
    void guiPlaceholderIsTextEvenWhenAPlotNameLooksLikeFormatting() {
        var gui = new GuiConfiguration();
        assertEquals("Lokal <red>nazwa</red>&a", PlainTextComponentSerializer.plainText().serialize(gui.render("<gold>Lokal {name}</gold>", Map.of("name", "<red>nazwa</red>&a"))));
    }

    @Test
    void relocatedButtonWithAnotherMaterialStillRequiresTheCurrentQuote() {
        var plot = TestFixtures.plot(UUID.randomUUID());
        plot.setOwnerUuid(null);
        plot.setStatus(PlotStatus.AVAILABLE);
        var manager = mock(PlotManager.class);
        when(manager.getPlot(plot.getId())).thenReturn(Optional.of(plot));
        when(manager.getPurchasePrice(plot)).thenReturn(plot.getPrice());
        var payments = mock(PaymentService.class);
        var gui = new GuiConfiguration();
        var custom = new YamlConfiguration();
        custom.set("menus.buy.buttons.confirm.slot", 5);
        custom.set("menus.buy.buttons.confirm.material", "DIAMOND");
        gui.load(custom);
        Presentation.install(gui);
        var listener = new PlotGUIListener(manager, payments, mock(PlotListGUI.class), mock(PlotDetailsGUI.class), mock(PlotConfirmBuyGUI.class), mock(PlotMembersGUI.class));
        var player = mock(Player.class);
        when(player.hasPermission(anyString())).thenReturn(true);
        var inventory = mock(Inventory.class);
        when(inventory.getHolder()).thenReturn(new PlotGUIHolder("PLOT_CONFIRM_BUY", plot.getId()));
        when(inventory.getSize()).thenReturn(27);
        var view = mock(InventoryView.class);
        when(view.getTopInventory()).thenReturn(inventory);
        var item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.DIAMOND);
        when(item.hasItemMeta()).thenReturn(true);
        var meta = mock(ItemMeta.class);
        when(item.getItemMeta()).thenReturn(meta);
        var data = mock(PersistentDataContainer.class);
        when(meta.getPersistentDataContainer()).thenReturn(data);
        when(data.get(any(), eq(PersistentDataType.STRING))).thenAnswer(call -> ((NamespacedKey) call.getArgument(0)).getKey().equals("action") ? "confirm" : plot.getPrice().toPlainString());
        var click = mock(InventoryClickEvent.class);
        when(click.getInventory()).thenReturn(inventory);
        when(click.getView()).thenReturn(view);
        when(click.getRawSlot()).thenReturn(5);
        when(click.getWhoClicked()).thenReturn(player);
        when(click.getCurrentItem()).thenReturn(item);
        listener.onInventoryClick(click);
        verify(payments).buyPlot(player, plot);
        clearInvocations(payments);
        when(item.hasItemMeta()).thenReturn(false);
        listener.onInventoryClick(click);
        verifyNoInteractions(payments);
    }

    @Test
    void customTierThresholdsDriveTheActualTaxCalculator() {
        var settings = TestFixtures.resource("config.yml");
        settings.set("tax.size-tier-limits", List.of(200));
        settings.set("tax.size-tier-multipliers", List.of(3.0));
        settings.set("tax.size-tier-overflow-multiplier", 9.0);
        Settings.install(settings);
        var plot = TestFixtures.plot(null);
        plot.setTaxMode(TaxMode.SIZE_TIERS);
        assertEquals(0, new BigDecimal("300").compareTo(new TaxCalculator().calculateTax(plot)));
    }

    @Test
    void changingRentalUnitChangesFuturePeriods() {
        var settings = TestFixtures.resource("config.yml");
        settings.set("rental-period-seconds.DAY", 172800);
        Settings.install(settings);
        assertEquals(Duration.ofDays(2), RentalPeriodUnit.DAY.getDuration());
    }

    @Test
    void decimalPrecisionAndMaximumAreEnforcedByMoneyParsing() {
        var settings = TestFixtures.resource("config.yml");
        settings.set("economy.decimal-places", 3);
        settings.set("economy.maximum-transaction-amount", "100");
        Settings.install(settings);
        assertEquals(new BigDecimal("0.125"), Money.parse("0.125"));
        assertThrows(NumberFormatException.class, () -> Money.parse("100.001"));
    }

    @Test
    void invalidSettingsKeepTheLastGoodSnapshot() {
        var previous = Settings.integer("paging.history");
        var invalid = TestFixtures.resource("config.yml");
        invalid.set("paging.history", 0);
        assertThrows(IllegalArgumentException.class, () -> Settings.install(invalid));
        assertEquals(previous, Settings.integer("paging.history"));
        var bad = TestFixtures.resource("config.yml");
        bad.set("tax.size-tier-limits", List.of(1000, 250));
        assertThrows(IllegalArgumentException.class, () -> Settings.install(bad));
    }

    @Test
    void moneyAndDateFormatsFollowTheCurrentSettings() {
        var settings = TestFixtures.resource("config.yml");
        settings.set("formatting.money-locale", "en-US");
        settings.set("formatting.date-pattern", "dd/MM/yyyy");
        settings.set("formatting.timezone", "UTC");
        Settings.install(settings);
        assertTrue(TestFixtures.config().formatMoney(new BigDecimal("12.5")).contains("12.50"));
        assertEquals("10/11/2026", TimeUtil.formatInstant(Instant.parse("2026-11-10T12:00:00Z")));
    }

    @Test
    void allStaticTextReferencesHaveBundledDefaultsAndNoSqlQueriesAreInTheCatalog() throws Exception {
        var messages = new MessageConfiguration(TestFixtures.config());
        var pattern = Pattern.compile("Texts\\.(?:raw|markup|component)\\(\"([^\"]+)\"\\s*[,)]");
        try (var files = Files.walk(Path.of("src/main/java"))) {
            for (var file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                var matcher = pattern.matcher(Files.readString(file));
                while (matcher.find()) assertTrue(messages.contains(matcher.group(1)), file + ": " + matcher.group(1));
            }
        }
        var language = ConfigurationFiles.defaults("lang.yml");
        for (String key : language.getKeys(true))
            if (language.isString(key))
                assertFalse(language.getString(key).matches("(?s).*(SELECT \\* FROM|INSERT INTO|CREATE TABLE).*"), key);
    }

    @Test
    void wrongYamlShapesAndMixedGuiSlotTypesAreRejected() {
        var badConfig = new YamlConfiguration();
        badConfig.set("economy", "wrong-section");
        assertThrows(IllegalArgumentException.class, () -> Settings.install(badConfig));
        var badGui = new YamlConfiguration();
        badGui.set("menus.market.content-slots", List.of(0, "one"));
        assertThrows(IllegalArgumentException.class, () -> new GuiConfiguration().load(badGui));
    }

    @Test
    void signLayoutsAreEditableAndRequireExactlyFourTextLines() {
        var settings = TestFixtures.resource("config.yml");
        settings.set("signs.layouts.sale", List.of("<green>Oferta {plot_id}</green>", "{plot_name}", "{price}", "Moja akcja"));
        Settings.install(settings);
        var manager = mock(PlotManager.class);
        var plot = TestFixtures.plot(null);
        plot.setName("<red>Test</red>");
        when(manager.getPurchasePrice(plot)).thenReturn(plot.getPrice());
        var service = new pl.riftplots.service.sign.PlotSignService(TestFixtures.config(), mock(pl.riftplots.storage.repository.PlotSignRepository.class), manager, new TaxCalculator());
        var lines = service.formatLines(new pl.riftplots.domain.sign.PlotSign("sign", plot.getId(), pl.riftplots.domain.sign.PlotSignType.FOR_SALE, "world", 0, 60, 0), plot);
        assertEquals("Oferta test_plot", PlainTextComponentSerializer.plainText().serialize(lines.getFirst()));
        assertEquals("<red>Test</red>", PlainTextComponentSerializer.plainText().serialize(lines.get(1)));
        settings.set("signs.layouts.sale", List.of("too short"));
        assertThrows(IllegalArgumentException.class, () -> Settings.install(settings));
    }

    @Test
    void defaultPropertyPolicyOnlyChangesNewModels() {
        var existing = TestFixtures.plot(null);
        var previousMember = PlotMember.defaultMember(UUID.randomUUID(), "Existing");
        var settings = TestFixtures.resource("config.yml");
        settings.set("plots.default-type", "SHOP");
        settings.set("rental.default-unit", "WEEK");
        settings.set("members.default-permissions", List.of("BUILD"));
        Settings.install(settings);
        assertEquals("SHOP", TestFixtures.plot(null).getPropertyType());
        assertEquals("WEEK", TestFixtures.plot(null).getRentalPeriodUnit());
        assertEquals("RESIDENTIAL", existing.getPropertyType());
        assertEquals("DAY", existing.getRentalPeriodUnit());
        assertEquals(Set.of(PlotPermission.BUILD), PlotMember.defaultMember(UUID.randomUUID(), "New").getPermissions());
        assertFalse(previousMember.hasPermission(PlotPermission.BUILD));
    }

    @Test
    void everyStaticSettingsReferenceHasADefault() throws Exception {
        var defaults = ConfigurationFiles.defaults("config.yml");
        var pattern = Pattern.compile("Settings\\.(?:integer|longValue|decimal|flag|string|strings|list|section)\\(\"([^\"]+)\"\\s*\\)");
        try (var files = Files.walk(Path.of("src/main/java"))) {
            for (var file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                var matcher = pattern.matcher(Files.readString(file));
                while (matcher.find()) assertTrue(defaults.contains(matcher.group(1)), file + ": " + matcher.group(1));
            }
        }
    }
}
