package com.pvz.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.pvz.builder.CustomMapBuilder;
import com.pvz.builder.GameMap;
import com.pvz.builder.LevelDirector;
import com.pvz.builder.MapMode;

/**
 * Unit tests for LevelDataLoader: the shipped campaign files and saved custom maps.
 */
class LevelDataLoaderTest {
    private static final Set<String> PLANTS = Set.copyOf(LevelDirector.PLANTS_USED);
    private final MapSerializer serializer = new MapSerializer(PLANTS);
    private final LevelDirector director = new LevelDirector();

    @Test
    @DisplayName("Campaign files load in order and match LevelDirector exactly")
    void testCampaignMatchesDirector(@TempDir Path root) {
        List<GameMap> levels = new LevelDataLoader(serializer, new UserDataPaths(root))
                .loadCampaignLevels();

        assertEquals(LevelDataLoader.CAMPAIGN_FILES.size(), levels.size());
        // If this fails, a level was changed in only one place: regenerate the JSON files
        // from LevelDirector, or update LevelDirector to match the files
        assertEquals(serializer.toJson(director.buildDayLevel(new CustomMapBuilder(PLANTS))),
                serializer.toJson(levels.get(0)));
        assertEquals(serializer.toJson(director.buildPoolLevel(new CustomMapBuilder(PLANTS))),
                serializer.toJson(levels.get(1)));
        for (GameMap level : levels) {
            assertEquals(MapMode.CAMPAIGN, level.getMode());
        }
    }

    @Test
    @DisplayName("Saved custom maps load; a broken file is reported, not fatal")
    void testCustomMaps(@TempDir Path root) throws Exception {
        UserDataPaths paths = new UserDataPaths(root);
        LevelDataLoader loader = new LevelDataLoader(serializer, paths);
        assertTrue(loader.loadCustomMaps().maps().isEmpty());

        GameMap lawn = new CustomMapBuilder(PLANTS).setName("San co").build();
        GameMap pool = director.buildPoolTemplate(new CustomMapBuilder(PLANTS).setName("Ho"));
        serializer.save(lawn, paths.mapFile(lawn.getName()));
        serializer.save(pool, paths.mapFile(pool.getName()));
        Path broken = paths.getMapsDir().resolve("broken.json");
        Files.writeString(broken, "{ not json");

        LevelDataLoader.CustomMaps result = loader.loadCustomMaps();

        assertEquals(List.of("Ho", "San co"),
                result.maps().stream().map(saved -> saved.map().getName()).toList());
        assertEquals(paths.mapFile("Ho"), result.maps().get(0).file());
        assertEquals(List.of(broken), result.unreadable());
    }

    @Test
    @DisplayName("Saving again under the same name overwrites the old file")
    void testOverwrite(@TempDir Path root) throws Exception {
        UserDataPaths paths = new UserDataPaths(root);
        serializer.save(new CustomMapBuilder(PLANTS).setName("A").setStartingSun(100).build(),
                paths.mapFile("A"));
        serializer.save(new CustomMapBuilder(PLANTS).setName("A").setStartingSun(200).build(),
                paths.mapFile("A"));

        LevelDataLoader.CustomMaps result =
                new LevelDataLoader(serializer, paths).loadCustomMaps();
        assertEquals(1, result.maps().size());
        assertEquals(200, result.maps().get(0).map().getStartingSun());
    }
}
