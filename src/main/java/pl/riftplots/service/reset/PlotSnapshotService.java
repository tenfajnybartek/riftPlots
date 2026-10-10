package pl.riftplots.service.reset;

import pl.riftplots.domain.plot.Plot;
import pl.riftplots.integration.worldedit.WorldEditHook;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class PlotSnapshotService {
    private final File schematicsDirectory;
    private final WorldEditHook worldEditHook;

    public PlotSnapshotService(File dataFolder, WorldEditHook worldEditHook) {
        this.schematicsDirectory = new File(dataFolder, "schematics");
        if (!schematicsDirectory.exists()) {
            schematicsDirectory.mkdirs();
        }
        this.worldEditHook = Objects.requireNonNull(worldEditHook, "worldEditHook cannot be null");
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
        boolean success = worldEditHook.saveSnapshot(plot.getBounds(), targetFile);
        if (success) {
            plot.setSchematicFile(targetFile.getName());
        }
        return success;
    }
}
