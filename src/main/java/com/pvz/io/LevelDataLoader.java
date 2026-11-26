package com.pvz.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.pvz.builder.GameMap;
import com.pvz.builder.LevelDirector;

/**
 * Lists the maps the level-select screen offers: the campaign levels shipped in the jar
 * and the custom maps the player saved.
 */
public final class LevelDataLoader {
    private static final String CAMPAIGN_DIR = "/levels/campaign/";

    /**
     * A folder inside a jar cannot be listed, so the campaign files are named here, in
     * play order. Adding a level means adding its file and one line here.
     */
    static final List<String> CAMPAIGN_FILES = List.of(
            "level_1_day.json",
            "level_2_pool.json");

    private final MapSerializer serializer;
    private final UserDataPaths paths;

    public LevelDataLoader(MapSerializer serializer, UserDataPaths paths) {
        this.serializer = serializer;
        this.paths = paths;
    }

    /** A loader for the real game: every known plant and the player's ~/.pvz folder. */
    public static LevelDataLoader createDefault() {
        // TODO(An Nguyen): take the plant IDs from PlantFactory once model/plant exists
        MapSerializer serializer = new MapSerializer(Set.copyOf(LevelDirector.PLANTS_USED));
        return new LevelDataLoader(serializer, UserDataPaths.inUserHome());
    }

    /**
     * @throws IllegalStateException if a level file is missing or broken; they ship with
     *                               the game, so that is a bug, not a player mistake
     */
    public List<GameMap> loadCampaignLevels() {
        List<GameMap> levels = new ArrayList<>();
        for (String file : CAMPAIGN_FILES) {
            String resource = CAMPAIGN_DIR + file;
            try (InputStream in = LevelDataLoader.class.getResourceAsStream(resource)) {
                if (in == null) {
                    throw new IllegalStateException("Missing campaign level " + resource);
                }
                String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                levels.add(serializer.fromJson(json));
            } catch (IOException | MapFormatException e) {
                throw new IllegalStateException("Broken campaign level " + resource, e);
            }
        }
        return levels;
    }

    /**
     * Loads every saved custom map. A broken file does not stop the others from loading;
     * it is listed in {@link CustomMaps#unreadable()} so the screen can say so.
     */
    public CustomMaps loadCustomMaps() throws IOException {
        List<SavedMap> maps = new ArrayList<>();
        List<Path> unreadable = new ArrayList<>();
        for (Path file : paths.listMapFiles()) {
            try {
                maps.add(new SavedMap(file, serializer.load(file)));
            } catch (IOException | MapFormatException e) {
                unreadable.add(file);
            }
        }
        return new CustomMaps(maps, unreadable);
    }

    /** A custom map and the file it came from, so the editor can open and overwrite it. */
    public record SavedMap(Path file, GameMap map) {
    }

    public record CustomMaps(List<SavedMap> maps, List<Path> unreadable) {
        public CustomMaps {
            maps = List.copyOf(maps);
            unreadable = List.copyOf(unreadable);
        }
    }
}
