package pl.riftplots.storage.repository;

import pl.riftplots.domain.plot.Plot;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlotRepository {
    List<Plot> findAll();
    Optional<Plot> findById(String id);
    List<Plot> findByOwner(UUID ownerUuid);
    void save(Plot plot);
    void saveAll(Iterable<Plot> plots);
    void delete(String id);
}
