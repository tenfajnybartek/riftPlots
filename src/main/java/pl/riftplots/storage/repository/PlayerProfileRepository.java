package pl.riftplots.storage.repository;

import pl.riftplots.domain.player.PlotPlayerProfile;

import java.util.Optional;
import java.util.UUID;

public interface PlayerProfileRepository {
    java.util.List<PlotPlayerProfile> findAll();

    Optional<PlotPlayerProfile> findById(UUID uuid);

    void save(PlotPlayerProfile profile);
}
