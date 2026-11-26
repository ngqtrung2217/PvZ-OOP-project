package com.pvz.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.pvz.model.board.GameBoard;
import com.pvz.model.tile.TileType;

/**
 * Unit tests for the built-in campaign levels made by LevelDirector.
 */
class LevelDirectorTest {
    private final LevelDirector director = new LevelDirector();
    private final MapValidator validator = new MapValidator(Set.of("PEASHOOTER", "SNOW_PEA"));
    private GameMap day;
    private GameMap pool;

    @BeforeEach
    void setUp() {
        day = director.buildDayLevel(newBuilder());
        pool = director.buildPoolLevel(newBuilder());
    }

    private static MapBuilder newBuilder() {
        return new CustomMapBuilder(Set.copyOf(LevelDirector.PLANTS_USED));
    }

    @Test
    @DisplayName("Both levels are campaign levels with the hard starting sun")
    void testCampaignMode() {
        for (GameMap level : List.of(day, pool)) {
            assertEquals(MapMode.CAMPAIGN, level.getMode());
            assertFalse(level.isEndless());
            assertEquals(50, level.getStartingSun());
        }
    }

    @Test
    @DisplayName("Both levels pass the editor rules without any warning")
    void testLevelsAreValid() {
        for (GameMap level : List.of(day, pool)) {
            ValidationResult result = validator.validate(level);
            assertTrue(result.canSave(), level.getName() + ": " + result.getErrors());
            assertTrue(result.getWarnings().isEmpty(), level.getName() + ": " + result.getWarnings());
        }
    }

    @Test
    @DisplayName("Day level has 5 waves, huge on 3 and 5, and no water")
    void testDayLevel() {
        assertEquals(5, day.getWaves().size());
        assertEquals(List.of(false, false, true, false, true), hugeFlags(day));
        assertEquals(0, count(day, TileType.WATER));
        assertTrue(count(day, TileType.CRATER) > 0);
        assertFalse(day.getAllowedPlants().contains("LILY_PAD"));
    }

    @Test
    @DisplayName("Pool level has 6 waves, huge on 3 and 6, and two full water rows")
    void testPoolLevel() {
        assertEquals(6, pool.getWaves().size());
        assertEquals(List.of(false, false, true, false, false, true), hugeFlags(pool));
        for (int c = 0; c < GameBoard.COLS; c++) {
            assertEquals(TileType.WATER, pool.getTileType(2, c));
            assertEquals(TileType.WATER, pool.getTileType(3, c));
        }
        assertEquals(2 * GameBoard.COLS, count(pool, TileType.WATER));
    }

    @Test
    @DisplayName("Water zombies only spawn on water lanes of the pool level")
    void testWaterZombiesOnWaterLanes() {
        for (WaveConfig wave : pool.getWaves()) {
            for (SpawnConfig spawn : wave.getSpawns()) {
                if (spawn.getZombieType().equals("WATER")) {
                    assertEquals(TileType.WATER, pool.getTileType(spawn.getLane(), 0));
                }
            }
        }
    }

    @Test
    @DisplayName("Every huge wave starts with a flag zombie")
    void testHugeWavesHaveFlag() {
        for (GameMap level : List.of(day, pool)) {
            for (WaveConfig wave : level.getWaves()) {
                if (wave.isHugeWave()) {
                    assertEquals("FLAG", wave.getSpawns().get(0).getZombieType());
                }
            }
        }
    }

    @Test
    @DisplayName("Each call builds a separate map")
    void testIndependentBuilds() {
        GameMap again = director.buildDayLevel(newBuilder());
        assertNotSame(day, again);
        assertEquals(day.getWaves().size(), again.getWaves().size());
    }

    @Test
    @DisplayName("Templates are endless custom maps that only need a name to be saved")
    void testTemplatesOnlyNeedName() {
        GameMap lawn = director.buildLawnTemplate(newBuilder());
        GameMap poolTemplate = director.buildPoolTemplate(newBuilder());

        for (GameMap template : List.of(lawn, poolTemplate)) {
            assertEquals(MapMode.CUSTOM, template.getMode());
            assertTrue(template.isEndless());
            assertTrue(template.getWaves().isEmpty());

            ValidationResult result = validator.validate(template);
            assertEquals(List.of(MapIssue.MISSING_NAME), result.getErrors());
            assertTrue(result.getWarnings().isEmpty(), result.getWarnings().toString());
        }
    }

    @Test
    @DisplayName("Lawn template is all grass; pool template has the pool level's water")
    void testTemplateTerrain() {
        GameMap lawn = director.buildLawnTemplate(newBuilder());
        GameMap poolTemplate = director.buildPoolTemplate(newBuilder());

        assertEquals(GameBoard.ROWS * GameBoard.COLS, count(lawn, TileType.GRASS));
        assertEquals(2 * GameBoard.COLS, count(poolTemplate, TileType.WATER));
        assertEquals(0, count(poolTemplate, TileType.CRATER));
        for (int c = 0; c < GameBoard.COLS; c++) {
            assertEquals(pool.getTileType(2, c), poolTemplate.getTileType(2, c));
            assertEquals(pool.getTileType(3, c), poolTemplate.getTileType(3, c));
        }
    }

    @Test
    @DisplayName("The editor can keep changing the builder after applying a template")
    void testTemplateBuilderStaysEditable() {
        MapBuilder builder = newBuilder();
        director.buildPoolTemplate(builder);

        GameMap edited = builder.setName("Ho cua toi").setMaxTurns(4).build();

        assertTrue(validator.validate(edited).canSave());
        assertEquals(TileType.WATER, edited.getTileType(2, 0));
        assertEquals(4, edited.getMaxTurns());
    }

    private static List<Boolean> hugeFlags(GameMap map) {
        return map.getWaves().stream().map(WaveConfig::isHugeWave).toList();
    }

    private static int count(GameMap map, TileType type) {
        int n = 0;
        for (int r = 0; r < GameBoard.ROWS; r++) {
            for (int c = 0; c < GameBoard.COLS; c++) {
                if (map.getTileType(r, c) == type) {
                    n++;
                }
            }
        }
        return n;
    }
}
