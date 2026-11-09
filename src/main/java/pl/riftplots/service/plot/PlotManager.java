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
    private PlayerProfileRepository profileRepository;
    private PropertyTypeManager typeManager;
    private DistrictManager districtManager;
    private Consumer<Plot> updateListener = plot -> {
    };

    public PlotManager(PlotRepository repository, WorldGuardHook worldGuardHook) {
        this.repository = Objects.requireNonNull(repository, "repository cannot be null");
        this.worldGuardHook = Objects.requireNonNull(worldGuardHook, "worldGuardHook cannot be null");
    }

    public void loadAll() {
        plots.clear();
        savedOwners.clear();
        List<Plot> list = repository.findAll();
        for (Plot plot : list) {
            plots.put(plot.getId(), plot);
            savedOwners.put(plot.getId(), plot.getOwnerUuid());
        }
    }

    public void configureRules(PropertyTypeManager typeManager, DistrictManager districtManager,
                               PlayerProfileRepository profileRepository) {
        this.typeManager = typeManager;
        this.districtManager = districtManager;
        this.profileRepository = profileRepository;
    }

    public void setUpdateListener(Consumer<Plot> updateListener) {
        this.updateListener = Objects.requireNonNull(updateListener);
    }

    public PlotValidationResult validateAcquisition(Plot plot, UUID buyerUuid) {
        if (plot == null || buyerUuid == null) return PlotValidationResult.error("Nieprawidłowy kupujący lub działka.");
        if (typeManager != null && !typeManager.getType(plot.getPropertyType()).isSalesAllowed()) {
            return PlotValidationResult.error("Ten typ nieruchomości nie pozwala na sprzedaż.");
        }
        if (districtManager != null && plot.getDistrictId() != null) {
            var district = districtManager.getDistrict(plot.getDistrictId());
            if (district.isEmpty()) return PlotValidationResult.error("Przypisana dzielnica nie istnieje.");
            var rules = district.get();
            if (!rules.isSalesEnabled() || !rules.isTypeAllowed(plot.getPropertyType())) {
                return PlotValidationResult.error("Dzielnica nie pozwala na sprzedaż tego typu nieruchomości.");
            }
            long count = getPlotsByOwner(buyerUuid).stream()
                    .filter(p -> !p.getId().equals(plot.getId()) && Objects.equals(p.getDistrictId(), plot.getDistrictId())).count();
            if (rules.getMaxPlotsPerPlayer() > 0 && count >= rules.getMaxPlotsPerPlayer()) {
                return PlotValidationResult.error("Osiągnięto limit działek w tej dzielnicy.");
            }
        }
        return PlotValidationResult.ok();
    }

    public int getMemberLimit(Plot plot) {
        return typeManager != null ? typeManager.getType(plot.getPropertyType()).getMaxMembers() : Integer.MAX_VALUE;
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
        for (Plot plot : plots.values()) {
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
        for (Plot plot : plots.values()) {
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
            return PlotValidationResult.error("Identyfikator działki nie może być pusty!");
        }
        String cleanId = id.toLowerCase();
        if (!cleanId.matches("^[a-zA-Z0-9_-]{2,32}$")) {
            return PlotValidationResult.error("Identyfikator może zawierać tylko litery, cyfry, podkreślenia i myślniki (2-32 znaki)!");
        }
        if (plots.containsKey(cleanId)) {
            return PlotValidationResult.error("Działka o identyfikatorze " + cleanId + " już istnieje!");
        }
        if (bounds == null) {
            return PlotValidationResult.error("Granice działki są nieprawidłowe!");
        }

        for (Plot existing : plots.values()) {
            if (existing.getBounds().intersects(bounds)) {
                return PlotValidationResult.error("Kolizja z istniejącą działką: " + existing.getId());
            }
        }

        String conflictRegion = worldGuardHook.findConflictingRegion(bounds, "riftplot_" + cleanId);
        if (conflictRegion != null) {
            return PlotValidationResult.error("Kolizja z istniejącym regionem WorldGuard: " + conflictRegion);
        }

        return PlotValidationResult.ok();
    }

    public boolean registerPlot(Plot plot) {
        if (plot == null) {
            return false;
        }
        if (!worldGuardHook.createOrUpdateRegion(plot)) {
            return false;
        }
        repository.save(plot);
        plots.put(plot.getId(), plot);
        savedOwners.put(plot.getId(), plot.getOwnerUuid());
        return true;
    }

    public void savePlot(Plot plot) {
        if (plot == null) {
            return;
        }
        plot.touch();
        plots.put(plot.getId(), plot);
        repository.save(plot);
        worldGuardHook.syncMembers(plot);
        synchronizeOwnerProfile(plot);
        updateListener.accept(plot);
    }

    private void synchronizeOwnerProfile(Plot plot) {
        UUID previous = savedOwners.put(plot.getId(), plot.getOwnerUuid());
        if (profileRepository == null || Objects.equals(previous, plot.getOwnerUuid())) return;
        if (previous != null) {
            profileRepository.findById(previous).ifPresent(profile -> {
                profile.removeOwnedPlot(plot.getId());
                profileRepository.save(profile);
            });
        }
        if (plot.getOwnerUuid() != null) {
            Instant now = Instant.now();
            PlotPlayerProfile profile = profileRepository.findById(plot.getOwnerUuid()).orElseGet(() ->
                    new PlotPlayerProfile(plot.getOwnerUuid(), plot.getOwnerName() != null ? plot.getOwnerName() : "Unknown",
                            now, now, new HashSet<>()));
            profile.addOwnedPlot(plot.getId());
            profileRepository.save(profile);
        }
    }

    public boolean deletePlot(String id) {
        if (id == null) {
            return false;
        }
        Plot plot = plots.remove(id.toLowerCase());
        if (plot == null) {
            return false;
        }
        UUID owner = savedOwners.remove(plot.getId());
        if (profileRepository != null && owner != null) {
            profileRepository.findById(owner).ifPresent(profile -> {
                profile.removeOwnedPlot(plot.getId());
                profileRepository.save(profile);
            });
        }
        worldGuardHook.removeRegion(plot.getBounds().getWorldName(), plot.getWorldGuardRegionId());
        repository.delete(id.toLowerCase());
        return true;
    }
}
