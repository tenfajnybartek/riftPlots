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
    public List<PlotPlayerProfile> findAll() {
        List<PlotPlayerProfile> profiles = new ArrayList<>();
        File[] files = playersDirectory.listFiles((directory, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String uuid = file.getName().substring(0, file.getName().length() - 4);
                findById(UUID.fromString(uuid)).ifPresent(profiles::add);
            }
        }
        return profiles;
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
            YamlConfiguration config = YamlDataFile.load(file);
            String lastName = config.getString("lastName", "Unknown");
            long firstJoin = config.getLong("firstJoin", System.currentTimeMillis());
            long lastSeen = config.getLong("lastSeen", System.currentTimeMillis());
            List<String> owned = config.getStringList("ownedPlots");
            return Optional.of(new PlotPlayerProfile(
                    uuid, lastName, Instant.ofEpochMilli(firstJoin), Instant.ofEpochMilli(lastSeen), new HashSet<>(owned)
            ));
        } catch (Exception e) {
            throw new IllegalStateException("Storage operation failed", e);
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
            YamlDataFile.save(config, file);
        } catch (IOException e) {
            throw new IllegalStateException("Storage operation failed", e);
        }
    }
}
