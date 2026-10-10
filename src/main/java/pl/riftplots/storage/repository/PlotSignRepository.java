package pl.riftplots.storage.repository;

import pl.riftplots.domain.sign.PlotSign;

import java.util.List;
import java.util.Optional;

public interface PlotSignRepository {
    List<PlotSign> findAll();
    Optional<PlotSign> findById(String id);
    Optional<PlotSign> findByLocation(String worldName, int x, int y, int z);
    List<PlotSign> findByPlotId(String plotId);
    void save(PlotSign sign);
    void delete(String id);
    void deleteByPlotId(String plotId);
}
