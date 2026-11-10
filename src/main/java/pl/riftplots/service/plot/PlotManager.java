package pl.riftplots.service.plot;

import pl.riftplots.domain.plot.Plot;
import pl.riftplots.domain.plot.PlotBounds;
import pl.riftplots.domain.plot.PlotStatus;
import pl.riftplots.integration.worldguard.WorldGuardHook;
import pl.riftplots.storage.repository.PlotRepository;
import pl.riftplots.storage.repository.PlayerProfileRepository;
import pl.riftplots.domain.player.PlotPlayerProfile;
import pl.riftplots.domain.type.PropertyTypeManager;
import pl.riftplots.service.district.DistrictManager;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class PlotManager {
    private final PlotRepository repository;
    private final WorldGuardHook worldGuardHook;
    private final Map<String, Plot> plots = new ConcurrentHashMap<>();
    private final Map<String, UUID> savedOwners = new HashMap<>();
    private final Map<String, Plot> committed = new HashMap<>();
    private final PlotSpatialIndex spatialIndex = new PlotSpatialIndex();
    private pl.riftplots.service.transaction.OperationJournal journal;
    private pl.riftplots.service.audit.AuditService audit;

    public record Actor(UUID uuid, String name) {
    }

    private final ThreadLocal<Actor> actor = new ThreadLocal<>();

    public Actor enterActor(org.bukkit.command.CommandSender sender) {
        Actor previous = actor.get();
        actor.set(new Actor(sender instanceof org.bukkit.entity.Player player ? player.getUniqueId() : null, sender.getName()));
        return previous;
    }

    public void restoreActor(Actor previous) {
        if (previous == null) actor.remove();
        else actor.set(previous);
    }

    public void setAuditService(pl.riftplots.service.audit.AuditService audit) {
        this.audit = audit;
    }

    public void setOperationJournal(pl.riftplots.service.transaction.OperationJournal journal) {
        this.journal = journal;
    }

    public String currentOperationId() {
        return journal != null ? journal.currentId() : null;
    }

    public boolean financiallyBlocked(String id, String allowed) {
        return journal != null && journal.isBlockedExcept(id, allowed);
    }

    private java.util.function.BooleanSupplier writeAllowed = () -> true;

    public void setWriteAllowed(java.util.function.BooleanSupplier allowed) {
        writeAllowed = allowed;
    }

    private java.util.function.Predicate<String> resetPending = id -> false;

    public void setResetPending(java.util.function.Predicate<String> resetPending) {
        this.resetPending = resetPending;
    }

    public boolean isResetPending(String id) {
        return resetPending.test(id);
    }

    private java.util.function.BiPredicate<String, UUID> tenantAccess = (plot, player) -> true;

    public void setTenantAccess(java.util.function.BiPredicate<String, UUID> tenantAccess) {
        this.tenantAccess = tenantAccess;
    }

    public boolean hasUsableMembership(Plot plot, UUID player) {
        var member = plot.getMember(player);
        return member != null && (member.getRole() != pl.riftplots.domain.plot.PlotMemberRole.TENANT || tenantAccess.test(plot.getId(), player));
    }

    private PlayerProfileRepository profileRepository;
    private PropertyTypeManager typeManager;
    private DistrictManager districtManager;
    private Consumer<Plot> updateListener = plot -> {
    };

    public PlotManager(PlotRepository repository, WorldGuardHook worldGuardHook) {
        this.repository = Objects.requireNonNull(repository, pl.riftplots.config.Texts.raw("text.plot-manager.repository-cannot-be-null"));
        this.worldGuardHook = Objects.requireNonNull(worldGuardHook, pl.riftplots.config.Texts.raw("text.plot-manager.worldguardhook-cannot-be-null"));
    }

    public void loadAll() {
        plots.clear();
        savedOwners.clear();
        committed.clear();
        spatialIndex.clear();
        List<Plot> list = repository.findAll();
        for (Plot plot : list) {
            plots.put(plot.getId(), plot);
            savedOwners.put(plot.getId(), plot.getOwnerUuid());
            committed.put(plot.getId(), plot.copy());
            spatialIndex.put(plot.getId(), plot.getBounds());
        }
    }

    public void configureRules(PropertyTypeManager typeManager, DistrictManager districtManager,
                               PlayerProfileRepository profileRepository) {
        this.typeManager = typeManager;
        this.districtManager = districtManager;
        this.profileRepository = profileRepository;
        refreshInheritedDefaults();
    }

    public void refreshInheritedDefaults() {
        for (Plot plot : plots.values()) if (refreshInherited(plot)) savePlot(plot);
    }

    private boolean refreshInherited(Plot plot) {
        if (typeManager == null || plot.getOwnerUuid() != null || plot.isRented() || plot.isAuctionActive()
                || plot.getStatus() != pl.riftplots.domain.plot.PlotStatus.AVAILABLE) return false;
        var type = typeManager.getType(plot.getPropertyType());
        var old = plot.copy();
        var mask = new HashSet<>(plot.getSettingOverrides());
        if (!plot.overrides(pl.riftplots.domain.plot.PropertySetting.PRICE)) plot.setPrice(type.getDefaultPrice());
        if (!plot.overrides(pl.riftplots.domain.plot.PropertySetting.BASE_TAX))
            plot.setBaseTax(type.getDefaultBaseTax());
        if (!plot.overrides(pl.riftplots.domain.plot.PropertySetting.TAX_MODE))
            plot.setTaxMode(type.getDefaultTaxMode());
        if (!plot.overrides(pl.riftplots.domain.plot.PropertySetting.TAX_MULTIPLIER))
            plot.setTaxMultiplier(type.getDefaultTaxMultiplier());
        if (!plot.overrides(pl.riftplots.domain.plot.PropertySetting.BILLING_PERIOD))
            plot.setBillingPeriodDays(type.getDefaultBillingPeriodDays());
        if (!plot.overrides(pl.riftplots.domain.plot.PropertySetting.REFUND_PERCENT))
            plot.setRefundPercentOnAbandon(type.getRefundPercent());
        plot.setSettingOverrides(mask);
        return old.getPrice().compareTo(plot.getPrice()) != 0 || old.getBaseTax().compareTo(plot.getBaseTax()) != 0
                || old.getTaxMode() != plot.getTaxMode() || old.getTaxMultiplier() != plot.getTaxMultiplier()
                || old.getBillingPeriodDays() != plot.getBillingPeriodDays() || old.getRefundPercentOnAbandon() != plot.getRefundPercentOnAbandon();
    }

    public void setUpdateListener(Consumer<Plot> updateListener) {
        this.updateListener = Objects.requireNonNull(updateListener);
    }

    public PlotValidationResult validateAcquisition(Plot plot, UUID buyerUuid) {
        if (plot == null || buyerUuid == null)
            return PlotValidationResult.error(pl.riftplots.config.Texts.raw("text.plot-manager.nieprawidowy-kupujacy-lub-dziaka"));
        if (isResetPending(plot.getId()))
            return PlotValidationResult.error(pl.riftplots.config.Texts.raw("text.plot-manager.dziaka-jest-przygotowywana-do-resetu"));
        if (financiallyBlocked(plot.getId(), currentOperationId()))
            return PlotValidationResult.error(pl.riftplots.config.Texts.raw("text.plot-manager.operacja-tej-nieruchomosci-wymaga-uzgodnienia-przez-administracje"));
        if (typeManager != null && !typeManager.getType(plot.getPropertyType()).isSalesAllowed()) {
            return PlotValidationResult.error(pl.riftplots.config.Texts.raw("text.plot-manager.ten-typ-nieruchomosci-nie-pozwala-na-sprzedaz"));
        }
        if (districtManager != null && plot.getDistrictId() != null) {
            var district = districtManager.getDistrict(plot.getDistrictId());
            if (district.isEmpty())
                return PlotValidationResult.error(pl.riftplots.config.Texts.raw("text.plot-manager.przypisana-dzielnica-nie-istnieje"));
            var rules = district.get();
            if (!rules.isSalesEnabled() || !rules.isTypeAllowed(plot.getPropertyType())) {
                return PlotValidationResult.error(pl.riftplots.config.Texts.raw("text.plot-manager.dzielnica-nie-pozwala-na-sprzedaz-tego-typu"));
            }
            long count = getPlotsByOwner(buyerUuid).stream()
                    .filter(p -> !p.getId().equals(plot.getId()) && Objects.equals(p.getDistrictId(), plot.getDistrictId())).count();
            if (rules.getMaxPlotsPerPlayer() > 0 && count >= rules.getMaxPlotsPerPlayer()) {
                return PlotValidationResult.error(pl.riftplots.config.Texts.raw("text.plot-manager.osiagnieto-limit-dziaek-w-tej-dzielnicy"));
            }
        }
        return PlotValidationResult.ok();
    }

    public int getMemberLimit(Plot plot) {
        return typeManager != null ? typeManager.getType(plot.getPropertyType()).getMaxMembers() : Integer.MAX_VALUE;
    }

    public java.math.BigDecimal getPurchasePrice(Plot plot) {
        if (plot.overrides(pl.riftplots.domain.plot.PropertySetting.PRICE) || typeManager == null)
            return plot.getPrice();
        var price = typeManager.getType(plot.getPropertyType()).getDefaultPrice();
        price = price.multiply(java.math.BigDecimal.valueOf(getDistrictPriceMultiplier(plot)));
        return price.setScale(pl.riftplots.config.Settings.integer("economy.decimal-places"), pl.riftplots.config.Settings.rounding());
    }

    public double getDistrictPriceMultiplier(Plot plot) {
        return districtManager != null && plot.getDistrictId() != null
                ? districtManager.getDistrict(plot.getDistrictId()).map(pl.riftplots.domain.district.District::getPriceMultiplier).orElse(1.0) : 1.0;
    }

    public org.bukkit.Material getIcon(Plot plot) {
        return typeManager != null ? typeManager.getType(plot.getPropertyType()).getIconMaterial() : org.bukkit.Material.matchMaterial(pl.riftplots.config.Settings.string("plots.default-icon"));
    }

    public void applyTypeDefaults(Plot plot) {
        if (typeManager == null)
            throw new IllegalStateException(pl.riftplots.config.Texts.raw("text.plot-manager.property-types-not-configured"));
        if (plot.isRented() || plot.isAuctionActive() || isResetPending(plot.getId()))
            throw new IllegalArgumentException(pl.riftplots.config.Texts.raw("text.plot-manager.zakoncz-najem-aukcje-lub-reset-przed-zmiana"));
        worldGuardHook.validateTypeFlags(typeManager.getType(plot.getPropertyType()).getDefaultFlags());
        plot.applyTypeDefaults(typeManager.getType(plot.getPropertyType()));
        savePlot(plot);
        worldGuardHook.applyTypeFlags(plot, typeManager.getAllTypes().stream().flatMap(type -> type.getDefaultFlags().keySet().stream()).collect(java.util.stream.Collectors.toSet()), typeManager.getType(plot.getPropertyType()).getDefaultFlags());
    }

    public Collection<Plot> getAllPlots() {
        return Collections.unmodifiableCollection(plots.values());
    }

    public Optional<Plot> getPlot(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(plots.get(id.toLowerCase()));
    }

    public Optional<Plot> getPlotAt(String worldName, int x, int y, int z) {
        if (worldName == null) {
            return Optional.empty();
        }
        for (String id : spatialIndex.candidates(worldName, x, z)) {
            Plot plot = plots.get(id);
            if (plot == null) continue;
            if (plot.getBounds().contains(worldName, x, y, z)) {
                return Optional.of(plot);
            }
        }
        return Optional.empty();
    }

    public Optional<Plot> getPlotAtHorizontal(String worldName, int x, int z) {
        if (worldName == null) {
            return Optional.empty();
        }
        for (String id : spatialIndex.candidates(worldName, x, z)) {
            Plot plot = plots.get(id);
            if (plot == null) continue;
            if (plot.getBounds().containsHorizontal(worldName, x, z)) {
                return Optional.of(plot);
            }
        }
        return Optional.empty();
    }

    public List<Plot> getPlotsByOwner(UUID ownerUuid) {
        if (ownerUuid == null) {
            return Collections.emptyList();
        }
        List<Plot> list = new ArrayList<>();
        for (Plot plot : plots.values()) {
            if (ownerUuid.equals(plot.getOwnerUuid())) {
                list.add(plot);
            }
        }
        return list;
    }

    public List<Plot> getPlotsByMember(UUID memberUuid) {
        if (memberUuid == null) {
            return Collections.emptyList();
        }
        List<Plot> list = new ArrayList<>();
        for (Plot plot : plots.values()) {
            if (plot.isMember(memberUuid)) {
                list.add(plot);
            }
        }
        return list;
    }

    public PlotValidationResult validateNewPlot(String id, PlotBounds bounds) {
        if (id == null || id.isBlank()) {
            return PlotValidationResult.error(pl.riftplots.config.Texts.raw("text.plot-manager.identyfikator-dziaki-nie-moze-byc-pusty"));
        }
        String cleanId = id.toLowerCase();
        if (!cleanId.matches("^[a-zA-Z0-9_-]{2,32}$")) {
            return PlotValidationResult.error(pl.riftplots.config.Texts.raw("text.plot-manager.identyfikator-moze-zawierac-tylko-litery-cyfry-podkreslenia"));
        }
        if (plots.containsKey(cleanId)) {
            return PlotValidationResult.error(pl.riftplots.config.Texts.raw("text.plot-manager.dziaka-o-identyfikatorze-juz-istnieje", cleanId));
        }
        if (bounds == null) {
            return PlotValidationResult.error(pl.riftplots.config.Texts.raw("text.plot-manager.granice-dziaki-sa-nieprawidowe"));
        }

        for (Plot existing : plots.values()) {
            if (existing.getBounds().intersects(bounds)) {
                return PlotValidationResult.error(pl.riftplots.config.Texts.raw("text.plot-manager.kolizja-z-istniejaca-dziaka", existing.getId()));
            }
        }

        String conflictRegion = worldGuardHook.findConflictingRegion(bounds, "riftplot_" + cleanId);
        if (conflictRegion != null) {
            return PlotValidationResult.error(pl.riftplots.config.Texts.raw("text.plot-manager.kolizja-z-istniejacym-regionem-worldguard", conflictRegion));
        }

        return PlotValidationResult.ok();
    }

    public boolean registerPlot(Plot plot) {
        if (!writeAllowed.getAsBoolean()) return false;
        if (plot == null) {
            return false;
        }
        refreshInherited(plot);
        if (!worldGuardHook.createOrUpdateRegion(plot)) {
            return false;
        }
        if (typeManager != null)
            worldGuardHook.applyTypeFlags(plot, typeManager.getAllTypes().stream().flatMap(type -> type.getDefaultFlags().keySet().stream()).collect(java.util.stream.Collectors.toSet()), typeManager.getType(plot.getPropertyType()).getDefaultFlags());
        repository.save(plot);
        plots.put(plot.getId(), plot);
        savedOwners.put(plot.getId(), plot.getOwnerUuid());
        committed.put(plot.getId(), plot.copy());
        spatialIndex.put(plot.getId(), plot.getBounds());
        return true;
    }

    public void savePlot(Plot plot) {
        if (plot == null) {
            return;
        }
        if (!writeAllowed.getAsBoolean()) {
            if (committed.containsKey(plot.getId())) plot.restore(committed.get(plot.getId()));
            throw new IllegalStateException(pl.riftplots.config.Texts.raw("text.plot-manager.zmiany-sa-wstrzymane-na-czas-migracji"));
        }
        refreshInherited(plot);
        plot.touch();
        Plot before = committed.get(plot.getId());
        try {
            repository.save(plot);
        } catch (RuntimeException e) {
            Plot previous = committed.get(plot.getId());
            if (previous != null) plot.restore(previous);
            throw e;
        }
        plots.put(plot.getId(), plot);
        committed.put(plot.getId(), plot.copy());
        spatialIndex.put(plot.getId(), plot.getBounds());
        if (!worldGuardHook.syncMembers(plot))
            throw new IllegalStateException(pl.riftplots.config.Texts.raw("text.plot-manager.nie-mozna-zsynchronizowac-ochrony-dziaki", plot.getId()));
        synchronizeOwnerProfile(plot);
        updateListener.accept(plot);
        if (audit != null && before != null && !auditState(before).equals(auditState(plot))) {
            Actor operator = actor.get();
            UUID id = operator != null ? operator.uuid() : journal != null && journal.currentId() != null ? journal.find(journal.currentId()).map(pl.riftplots.service.transaction.OperationJournal.Entry::actor).orElse(null) : null;
            audit.log(plot.getId(), id, operator != null ? operator.name() : pl.riftplots.config.Texts.raw("text.plot-manager.system"), "PLOT_CHANGED", auditState(before) + " -> " + auditState(plot) + " [op:" + currentOperationId() + "]");
        }
    }

    private String auditState(Plot plot) {
        String members = plot.getMembers().values().stream().sorted(java.util.Comparator.comparing(member -> member.getPlayerUuid().toString()))
                .map(member -> member.getPlayerUuid() + ":" + member.getRole() + member.getPermissions()).collect(java.util.stream.Collectors.joining(","));
        return plot.getStatus() + " owner=" + plot.getOwnerUuid() + " type=" + plot.getPropertyType() + " district=" + plot.getDistrictId()
                + " price=" + plot.getPrice() + " tax=" + plot.getBaseTax() + "/" + plot.getTaxMode() + "/" + plot.getTaxMultiplier()
                + " period=" + plot.getBillingPeriodDays() + " refund=" + plot.getRefundPercentOnAbandon() + " overrides=" + plot.getSettingOverrides() + " members=[" + members + "]"
                + " name=" + plot.getName() + " description=" + plot.getDescription() + " bounds=" + plot.getBounds()
                + " tax-enabled=" + plot.isTaxEnabled() + " grace-hours=" + plot.getGracePeriodHours()
                + " next-tax=" + plot.getNextTaxPayment() + " grace-end=" + plot.getGracePeriodExpiry()
                + " rental=" + plot.isRentalAvailable() + "/" + plot.getRentalPrice() + "/" + plot.getRentalPeriodUnit() + "/" + plot.getRentalDeposit() + "/" + plot.getActiveRentalId()
                + " auction=" + plot.isAuctionActive() + " resell=" + plot.isResellAvailable() + "/" + plot.getResellPrice()
                + " home=" + plot.getHomeWorld() + "/" + plot.getHomeX() + "/" + plot.getHomeY() + "/" + plot.getHomeZ() + "/" + plot.getHomeYaw() + "/" + plot.getHomePitch()
                + " schematic=" + plot.getSchematicFile() + " auto-reset=" + plot.isAutoResetOnExpire();
    }

    private void synchronizeOwnerProfile(Plot plot) {
        UUID previous = savedOwners.get(plot.getId());
        if (profileRepository == null || Objects.equals(previous, plot.getOwnerUuid())) {
            savedOwners.put(plot.getId(), plot.getOwnerUuid());
            return;
        }
        if (previous != null) {
            profileRepository.findById(previous).ifPresent(profile -> {
                profile.removeOwnedPlot(plot.getId());
                profileRepository.save(profile);
            });
        }
        if (plot.getOwnerUuid() != null) {
            Instant now = Instant.now();
            PlotPlayerProfile profile = profileRepository.findById(plot.getOwnerUuid()).orElseGet(() ->
                    new PlotPlayerProfile(plot.getOwnerUuid(), plot.getOwnerName() != null ? plot.getOwnerName() : pl.riftplots.config.Texts.raw("text.plot-manager.unknown"),
                            now, now, new HashSet<>()));
            profile.addOwnedPlot(plot.getId());
            profileRepository.save(profile);
        }
        savedOwners.put(plot.getId(), plot.getOwnerUuid());
    }

    public boolean deletePlot(String id) {
        if (!writeAllowed.getAsBoolean())
            throw new IllegalStateException(pl.riftplots.config.Texts.raw("text.plot-manager.zmiany-sa-wstrzymane-na-czas-migracji"));
        if (id == null) {
            return false;
        }
        Plot plot = plots.get(id.toLowerCase());
        if (plot == null) {
            return false;
        }
        repository.delete(plot.getId());
        plots.remove(plot.getId());
        committed.remove(plot.getId());
        spatialIndex.remove(plot.getId());
        worldGuardHook.removeRegion(plot.getBounds().getWorldName(), plot.getWorldGuardRegionId());
        UUID owner = savedOwners.remove(plot.getId());
        if (profileRepository != null && owner != null) {
            profileRepository.findById(owner).ifPresent(profile -> {
                profile.removeOwnedPlot(plot.getId());
                profileRepository.save(profile);
            });
        }
        return true;
    }
}
