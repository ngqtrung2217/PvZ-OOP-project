package com.pvz.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.pvz.builder.GameMap;
import com.pvz.builder.LevelDirector;
import com.pvz.builder.MapIssue;
import com.pvz.builder.MapMode;
import com.pvz.core.GameManager;
import com.pvz.io.MapSerializer;
import com.pvz.io.UserDataPaths;
import com.pvz.model.board.GameBoard;
import com.pvz.model.tile.TileType;

/**
 * Unit tests for MapEditorController: every editor action without any drawing.
 */
class MapEditorControllerTest {
    private static final List<String> PLANTS = LevelDirector.PLANTS_USED;

    @TempDir
    Path root;
    private UserDataPaths paths;
    private MapSerializer serializer;
    private MapEditorController editor;

    @BeforeEach
    void setUp() {
        paths = new UserDataPaths(root);
        serializer = new MapSerializer(Set.copyOf(PLANTS));
        editor = new MapEditorController(PLANTS, Set.of("PEASHOOTER", "SNOW_PEA", "CHERRY_BOMB"),
                serializer, paths);
    }

    @Test
    @DisplayName("A new map is all grass, every plant ticked, endless, no name")
    void testDefaults() {
        assertEquals(GameBoard.ROWS * GameBoard.COLS, editor.count(TileType.GRASS));
        assertEquals("", editor.getName());
        assertEquals(GameManager.STARTING_SUN, editor.getStartingSun());
        assertTrue(editor.isEndless());
        assertEquals(PLANTS, editor.getAllowedPlants());
        // Only the name is missing
        assertEquals(List.of(MapIssue.MISSING_NAME), editor.validate().getErrors());
    }

    @Test
    @DisplayName("Painting uses the brush; cells outside the grid are ignored")
    void testPaint() {
        editor.setBrush(TileType.CRATER);
        editor.paint(1, 2);
        editor.paint(-1, 0);
        editor.paint(0, GameBoard.COLS);

        assertEquals(TileType.CRATER, editor.getTile(1, 2));
        assertEquals(1, editor.count(TileType.CRATER));
        assertThrows(IllegalArgumentException.class, () -> editor.setBrush(null));
    }

    @Test
    @DisplayName("Templates change the terrain only; every other setting is kept")
    void testTemplatesOnlyChangeTerrain() {
        editor.setName("Ao cua toi");
        editor.changeSun(2);
        editor.togglePlant("SNOW_PEA");
        editor.changeTurns(1);
        editor.setBrush(TileType.CRATER);
        editor.paint(0, 0);
        List<String> plants = editor.getAllowedPlants();

        editor.applyPoolTemplate();

        assertEquals(2 * GameBoard.COLS, editor.count(TileType.WATER));
        assertEquals(0, editor.count(TileType.CRATER), "Painted terrain is replaced");
        assertEquals("Ao cua toi", editor.getName());
        assertEquals(100, editor.getStartingSun());
        assertEquals(plants, editor.getAllowedPlants());
        assertFalse(editor.isEndless());
        assertEquals(6, editor.getTurns());

        editor.applyDayTemplate();
        assertEquals(GameBoard.ROWS * GameBoard.COLS, editor.count(TileType.GRASS));
        assertEquals("Ao cua toi", editor.getName());
    }

    @Test
    @DisplayName("Typing builds the name, skips control characters and stops at the limit")
    void testNameTyping() {
        editor.typeName("Ao");
        editor.typeName("\r");
        editor.typeName(" sâu");
        assertEquals("Ao sâu", editor.getName());

        editor.deleteLastNameChar();
        assertEquals("Ao sâ", editor.getName());

        editor.setName("");
        editor.deleteLastNameChar();
        assertEquals("", editor.getName());

        editor.typeName("x".repeat(100));
        assertEquals(MapEditorController.MAX_NAME_LENGTH, editor.getName().length());
    }

    @Test
    @DisplayName("Sun moves in steps of 25 and stays within 0 and the maximum")
    void testSun() {
        editor.changeSun(4);
        assertEquals(150, editor.getStartingSun());
        editor.changeSun(-100);
        assertEquals(0, editor.getStartingSun());
        editor.changeSun(1000);
        assertEquals(MapEditorController.MAX_SUN, editor.getStartingSun());
    }

    @Test
    @DisplayName("Score multiplier follows clamp(150 / sun, 0.5, 2.0), even at 0 sun")
    void testSunMultiplier() {
        editor.changeSun(-100);
        assertEquals(2.0, editor.getSunMultiplier(), 1e-9);
        editor.changeSun(6); // 150
        assertEquals(1.0, editor.getSunMultiplier(), 1e-9);
        editor.changeSun(2); // 200
        assertEquals(0.75, editor.getSunMultiplier(), 1e-9);
        editor.changeSun(100);
        assertEquals(0.5, editor.getSunMultiplier(), 1e-9);
    }

    @Test
    @DisplayName("Plants toggle and always come out in seed-bar order")
    void testPlants() {
        editor.togglePlant("PEASHOOTER");
        assertFalse(editor.isPlantAllowed("PEASHOOTER"));
        editor.togglePlant("LILY_PAD");
        assertFalse(editor.isPlantAllowed("LILY_PAD"));
        editor.togglePlant("LILY_PAD");
        editor.togglePlant("PEASHOOTER");

        // Ticked last, yet Peashooter is back in its seed-bar place
        assertEquals(PLANTS, editor.getAllowedPlants());
        assertThrows(IllegalArgumentException.class, () -> editor.togglePlant("CACTUS"));
    }

    @Test
    @DisplayName("Changing turns leaves endless mode; the count stays between 1 and 99")
    void testTurns() {
        editor.changeTurns(-10);
        assertFalse(editor.isEndless());
        assertEquals(1, editor.getTurns());
        editor.changeTurns(500);
        assertEquals(MapEditorController.MAX_TURNS, editor.getTurns());

        editor.toggleEndless();
        assertTrue(editor.isEndless());
        assertTrue(editor.buildMap().isEndless());
        editor.toggleEndless();
        assertEquals(MapEditorController.MAX_TURNS, editor.buildMap().getMaxTurns());
    }

    @Test
    @DisplayName("The built map is a custom map with every setting")
    void testBuildMap() {
        editor.setName("Test");
        editor.setBrush(TileType.WATER);
        editor.paint(0, 0);
        editor.changeSun(2);
        editor.changeTurns(1);

        GameMap map = editor.buildMap();

        assertEquals(MapMode.CUSTOM, map.getMode());
        assertEquals("Test", map.getName());
        assertEquals(TileType.WATER, map.getTileType(0, 0));
        assertEquals(100, map.getStartingSun());
        assertEquals(6, map.getMaxTurns());
        assertEquals(editor.getAllowedPlants(), map.getAllowedPlants());
    }

    @Test
    @DisplayName("Save is blocked without a name and says why")
    void testSaveBlocked() throws Exception {
        assertNull(editor.save());
        assertTrue(editor.getMessage().startsWith("Chưa lưu"));
        assertTrue(editor.getMessage().contains(MapIssue.MISSING_NAME.getMessage()));
        assertTrue(paths.listMapFiles().isEmpty());
    }

    @Test
    @DisplayName("Save writes the file, a second save overwrites it, and open restores it")
    void testSaveAndOpen() throws Exception {
        editor.setName("Ao nha");
        editor.applyPoolTemplate();
        editor.changeSun(4);

        Path file = editor.save();
        assertNotNull(file);
        assertTrue(Files.exists(file));
        assertEquals("Đã lưu: Ao_nha.json", editor.getMessage());

        editor.save();
        assertEquals("Đã ghi đè: Ao_nha.json", editor.getMessage());

        MapEditorController other = new MapEditorController(PLANTS,
                Set.of("PEASHOOTER"), serializer, paths);
        List<com.pvz.io.LevelDataLoader.SavedMap> saved = other.listSavedMaps();
        assertEquals(1, saved.size());
        other.open(saved.get(0).map());

        assertEquals("Ao nha", other.getName());
        assertEquals(150, other.getStartingSun());
        assertEquals(2 * GameBoard.COLS, other.count(TileType.WATER));
        assertTrue(other.isPlantAllowed("LILY_PAD"));
        assertEquals("Đã mở: Ao nha", other.getMessage());
    }

    @Test
    @DisplayName("A name with no usable file characters is reported, not thrown")
    void testUnusableName() {
        editor.setName("???");
        assertNull(editor.save());
        assertEquals("Tên map không dùng làm tên file được", editor.getMessage());
    }

    @Test
    @DisplayName("Broken saved files are skipped and noted")
    void testListSkipsBroken() throws Exception {
        Files.createDirectories(paths.getMapsDir());
        Files.writeString(paths.getMapsDir().resolve("hong.json"), "{ not json");

        assertTrue(editor.listSavedMaps().isEmpty());
        assertEquals("1 file map bị hỏng nên đã bỏ qua", editor.getMessage());
    }

    @Test
    @DisplayName("Loading a limited map turns endless off and keeps its turn count")
    void testLoadLimitedMap() {
        editor.changeTurns(2); // 7 turns, not endless
        GameMap limited = editor.buildMap();
        editor.toggleEndless();

        editor.load(limited);

        assertFalse(editor.isEndless());
        assertEquals(7, editor.getTurns());
    }
}
