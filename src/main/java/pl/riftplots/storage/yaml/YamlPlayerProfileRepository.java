package pl.riftplots.storage.yaml;

import org.bukkit.configuration.file.YamlConfiguration;
import pl.riftplots.domain.player.PlotPlayerProfile;
import pl.riftplots.storage.repository.PlayerProfileRepository;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.*;

public class YamlPlayerProfileRepository implements PlayerProfileRepository {
    private final File playersDirectory;

    public YamlPlayerProfileRepository(File playersDirectory) {
        this.playersDirectory = playersDirectory;
        if (!playersDirectory.exists()) {
            playersDirectory.mkdirs();
        }
    }

    @Override
    public Optional<PlotPlayerProfile> findById(UUID uuid) {
        if (uuid == null) {
            return Optional.empty();
        }
        File file = new File(playersDirectory, uuid + ".yml");
        if (!file.exists()) {
            return Optional.empty();
        }
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            String lastName = config.getString("lastName", "Unknown");
            long firstJoin = config.getLong("firstJoin", System.currentTimeMillis());
            long lastSeen = config.getLong("lastSeen", System.currentTimeMillis());
            List<String> owned = config.getStringList("ownedPlots");
            return Optional.of(new PlotPlayerProfile(
                    uuid, lastName, Instant.ofEpochMilli(firstJoin), Instant.ofEpochMilli(lastSeen), new HashSet<>(owned)
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    @Override
    public void save(PlotPlayerProfile profile) {
        if (profile == null) {
            return;
        }
        File file = new File(playersDirectory, profile.getUuid() + ".yml");
        YamlConfiguration config = new YamlConfiguration();
        config.set("uuid", profile.getUuid().toString());
        config.set("lastName", profile.getLastName());
        config.set("firstJoin", profile.getFirstJoin().toEpochMilli());
        config.set("lastSeen", profile.getLastSeen().toEpochMilli());
        config.set("ownedPlots", new ArrayList<>(profile.getOwnedPlotIds()));

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
