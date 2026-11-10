package pl.riftplots.service.reset;

import pl.riftplots.domain.plot.Plot;
import pl.riftplots.integration.worldedit.WorldEditHook;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.util.Objects;

public class PlotSnapshotService {
    private final File schematicsDirectory;
    private final WorldEditHook worldEditHook;

    public PlotSnapshotService(File dataFolder, WorldEditHook worldEditHook) {
        this.schematicsDirectory = new File(dataFolder, "schematics");
        if (!schematicsDirectory.exists()) {
            schematicsDirectory.mkdirs();
        }
        this.worldEditHook = Objects.requireNonNull(worldEditHook, pl.riftplots.config.Texts.raw("text.plot-snapshot-service.worldedithook-cannot-be-null"));
    }

    public File getSchematicFile(String plotId) {
        if (plotId == null) {
            return null;
        }
        return new File(schematicsDirectory, plotId.toLowerCase() + ".schem");
    }

    public boolean hasSnapshot(String plotId) {
        File file = getSchematicFile(plotId);
        return file != null && file.exists() && file.length() > 0;
    }

    public boolean createSnapshot(Plot plot) throws IOException {
        if (plot == null) {
            return false;
        }
        File targetFile = getSchematicFile(plot.getId());
        File temporaryFile = new File(schematicsDirectory, plot.getId() + ".schem.tmp");
        try {
            if (!worldEditHook.saveSnapshot(plot.getBounds(), temporaryFile)) return false;
            try {
                Files.move(temporaryFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporaryFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            plot.setSchematicFile(targetFile.getName());
            return true;
        } finally {
            Files.deleteIfExists(temporaryFile.toPath());
        }
    }
}
