package com.pvz.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.pvz.builder.CustomMapBuilder;
import com.pvz.builder.GameMap;
import com.pvz.builder.LevelDirector;
import com.pvz.builder.MapBuilder;
import com.pvz.builder.MapMode;
import com.pvz.model.tile.TileType;

/**
 * Unit tests for MapSerializer: round trips, the README file format and bad files.
 */
class JsonSerializationTest {
    private static final Set<String> PLANTS = Set.copyOf(LevelDirector.PLANTS_USED);
    private static final String GRASS_ROW =
            "[\"GRASS\",\"GRASS\",\"GRASS\",\"GRASS\",\"GRASS\",\"GRASS\",\"GRASS\",\"GRASS\",\"GRASS\"]";

    private final MapSerializer serializer = new MapSerializer(PLANTS);
    private final LevelDirector director = new LevelDirector();

    private static MapBuilder newBuilder() {
        return new CustomMapBuilder(PLANTS);
    }

    /** The custom map example from README section 3.10. */
    private static final String README_EXAMPLE = """
            {
              "name": "Ao giữa sân",
              "mode": "CUSTOM",
              "tiles": [
                ["GRASS","GRASS","WATER","GRASS","GRASS","GRASS","GRASS","GRASS","GRASS"],
                ["GRASS","WATER","WATER","GRASS","WATER","GRASS","GRASS","GRASS","GRASS"],
                ["GRASS","GRASS","GRASS","GRASS","CRATER","WATER","GRASS","GRASS","GRASS"],
                ["GRASS","GRASS","GRASS","GRASS","CRATER","GRASS","GRASS","GRASS","GRASS"],
                ["GRASS","GRASS","GRASS","GRASS","GRASS","GRASS","GRASS","GRASS","GRASS"]
              ],
              "startingSun": 150,
              "allowedPlants": ["PEASHOOTER", "SUNFLOWER", "WALL_NUT", "LILY_PAD"],
              "maxTurns": 5
            }
            """;

    /** A minimal valid custom map with the given extra fields appended. */
    private static String customJson(String extraFields) {
        return "{\"tiles\":[" + String.join(",", GRASS_ROW, GRASS_ROW, GRASS_ROW, GRASS_ROW,
                GRASS_ROW) + "],\"startingSun\":50" + extraFields + "}";
    }

    @Test
    @DisplayName("Every built-in level and template survives save and load unchanged")
    void testRoundTrip() throws Exception {
        List<GameMap> maps = List.of(
                director.buildDayLevel(newBuilder()),
                director.buildPoolLevel(newBuilder()),
                director.buildLawnTemplate(newBuilder()),
                director.buildPoolTemplate(newBuilder()));

        for (GameMap original : maps) {
            String json = serializer.toJson(original);
            GameMap loaded = serializer.fromJson(json);

            assertEquals(json, serializer.toJson(loaded));
            assertEquals(original.getMode(), loaded.getMode());
            assertEquals(original.getWaves().size(), loaded.getWaves().size());
            assertEquals(MapIdHasher.hash(original), MapIdHasher.hash(loaded));
        }
    }

    @Test
    @DisplayName("The README example file loads with all its values")
    void testReadmeExample() throws Exception {
        GameMap map = serializer.fromJson(README_EXAMPLE);

        assertEquals("Ao giữa sân", map.getName());
        assertEquals(MapMode.CUSTOM, map.getMode());
        assertEquals(TileType.WATER, map.getTileType(0, 2));
        assertEquals(TileType.CRATER, map.getTileType(2, 4));
        assertEquals(TileType.GRASS, map.getTileType(4, 8));
        assertEquals(150, map.getStartingSun());
        assertEquals(List.of("PEASHOOTER", "SUNFLOWER", "WALL_NUT", "LILY_PAD"),
                map.getAllowedPlants());
        assertEquals(5, map.getMaxTurns());
    }

    @Test
    @DisplayName("maxTurns null or missing both mean endless")
    void testEndless() throws Exception {
        assertTrue(serializer.fromJson(customJson(",\"maxTurns\":null")).isEndless());
        assertTrue(serializer.fromJson(customJson("")).isEndless());
        assertNull(serializer.fromJson(customJson("")).getMaxTurns());
    }

    @Test
    @DisplayName("Missing name and plants load as blank, for the validator to judge")
    void testMissingOptionalFields() throws Exception {
        GameMap map = serializer.fromJson(customJson(""));
        assertEquals("", map.getName());
        assertTrue(map.getAllowedPlants().isEmpty());
    }

    @Test
    @DisplayName("Custom files have no waves field; campaign files do")
    void testWavesOnlyInCampaign() {
        assertFalse(serializer.toJson(director.buildLawnTemplate(newBuilder())).contains("\"waves\""));
        assertTrue(serializer.toJson(director.buildDayLevel(newBuilder())).contains("\"waves\""));
    }

    @Test
    @DisplayName("save and load through a file keep Vietnamese text")
    void testSaveLoadFile(@TempDir Path dir) throws Exception {
        GameMap map = newBuilder().setName("Bể bơi đêm").build();
        Path file = dir.resolve("nested/map.json");

        serializer.save(map, file);

        assertEquals("Bể bơi đêm", serializer.load(file).getName());
    }

    @Test
    @DisplayName("Broken JSON and empty text are rejected")
    void testNotJson() {
        assertThrows(MapFormatException.class, () -> serializer.fromJson("{ not json"));
        assertThrows(MapFormatException.class, () -> serializer.fromJson(""));
    }

    @Test
    @DisplayName("A wrong grid shape or unknown tile is rejected")
    void testBadTiles() {
        assertThrows(MapFormatException.class,
                () -> serializer.fromJson("{\"startingSun\":50}"));
        assertThrows(MapFormatException.class,
                () -> serializer.fromJson("{\"startingSun\":50,\"tiles\":[" + GRASS_ROW + "]}"));
        assertThrows(MapFormatException.class,
                () -> serializer.fromJson(README_EXAMPLE.replace("\"WATER\",\"GRASS\",\"GRASS\",\"GRASS\",\"GRASS\",\"GRASS\",\"GRASS\"]",
                        "\"WATER\",\"GRASS\",\"GRASS\",\"GRASS\",\"GRASS\",\"GRASS\"]")));
        assertThrows(MapFormatException.class,
                () -> serializer.fromJson(README_EXAMPLE.replace("CRATER", "LAVA")));
        assertThrows(MapFormatException.class,
                () -> serializer.fromJson(README_EXAMPLE.replaceFirst("\"GRASS\"", "null")));
    }

    @Test
    @DisplayName("Missing or negative sun is rejected")
    void testBadSun() {
        assertThrows(MapFormatException.class,
                () -> serializer.fromJson(README_EXAMPLE.replace("\"startingSun\": 150,", "")));
        assertThrows(MapFormatException.class,
                () -> serializer.fromJson(README_EXAMPLE.replace("150", "-10")));
    }

    @Test
    @DisplayName("Unknown or null plants and invalid turn counts are rejected")
    void testBadPlantsAndTurns() {
        assertThrows(MapFormatException.class,
                () -> serializer.fromJson(README_EXAMPLE.replace("WALL_NUT", "CACTUS")));
        assertThrows(MapFormatException.class,
                () -> serializer.fromJson(README_EXAMPLE.replace("\"WALL_NUT\"", "null")));
        assertThrows(MapFormatException.class,
                () -> serializer.fromJson(README_EXAMPLE.replace("\"maxTurns\": 5", "\"maxTurns\": 0")));
    }

    @Test
    @DisplayName("Bad waves are rejected: unknown zombie, missing lane, lane out of range")
    void testBadWaves() {
        String wave = ",\"waves\":[{\"hugeWave\":false,\"spawns\":[%s]}]";
        assertThrows(MapFormatException.class, () -> serializer.fromJson(customJson(
                wave.formatted("{\"zombieType\":\"DRAGON\",\"lane\":0,\"spawnDelay\":1}"))));
        assertThrows(MapFormatException.class, () -> serializer.fromJson(customJson(
                wave.formatted("{\"zombieType\":\"NORMAL\",\"spawnDelay\":1}"))));
        assertThrows(MapFormatException.class, () -> serializer.fromJson(customJson(
                wave.formatted("{\"zombieType\":\"NORMAL\",\"lane\":9,\"spawnDelay\":1}"))));
        assertThrows(MapFormatException.class, () -> serializer.fromJson(customJson(
                ",\"waves\":[{\"hugeWave\":false,\"spawns\":[]}]")));
        assertThrows(MapFormatException.class, () -> serializer.fromJson(customJson(
                ",\"waves\":[null]")));
    }

    @Test
    @DisplayName("A mode that does not match the content is rejected")
    void testModeMismatch() {
        assertThrows(MapFormatException.class,
                () -> serializer.fromJson(README_EXAMPLE.replace("\"CUSTOM\"", "\"CAMPAIGN\"")));
        assertThrows(MapFormatException.class,
                () -> serializer.fromJson(README_EXAMPLE.replace("\"CUSTOM\"", "\"ARCADE\"")));
    }

    @Test
    @DisplayName("A campaign file that also sets maxTurns is rejected")
    void testCampaignWithMaxTurns() {
        String json = serializer.toJson(director.buildDayLevel(newBuilder()))
                .replace("\"startingSun\": 50,", "\"startingSun\": 50, \"maxTurns\": 3,");
        assertThrows(MapFormatException.class, () -> serializer.fromJson(json));
    }
}
