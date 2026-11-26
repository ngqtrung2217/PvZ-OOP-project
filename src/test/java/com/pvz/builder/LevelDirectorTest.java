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
 * Unit tests for LevelDirector: the three campaign levels and the two terrain templates.
 */
class LevelDirectorTest {
    private final LevelDirector director = new LevelDirector();
    private final MapValidator validator =
            new MapValidator(Set.of("PEASHOOTER", "SNOW_PEA", "CHERRY_BOMB"));
    private GameMap easy;
    private GameMap medium;
    private GameMap hard;

    @BeforeEach
    void setUp() {
        easy = director.buildEasyLevel(newBuilder());
        medium = director.buildMediumLevel(newBuilder());
        hard = director.buildHardLevel(newBuilder());
    }

    private static MapBuilder newBuilder() {
        return new CustomMapBuilder(Set.copyOf(LevelDirector.PLANTS_USED));
    }

    private List<GameMap> levels() {
        return List.of(easy, medium, hard);
    }

    @Test
    @DisplayName("All three levels are campaign levels that pass the editor rules cleanly")
    void testLevelsAreValidCampaigns() {
        for (GameMap level : levels()) {
            assertEquals(MapMode.CAMPAIGN, level.getMode());
            assertFalse(level.isEndless());
            ValidationResult result = validator.validate(level);
            assertTrue(result.canSave(), level.getName() + ": " + result.getErrors());
            assertTrue(result.getWarnings().isEmpty(), level.getName() + ": " + result.getWarnings());
        }
    }

    @Test
    @DisplayName("Names say the difficulty and nothing is called super hard any more")
    void testNames() {
        assertEquals("Ban ngày (Dễ)", easy.getName());
        assertEquals("Sân hố (Trung bình)", medium.getName());
        assertEquals("Bể bơi (Khó)", hard.getName());
        for (GameMap level : levels()) {
            assertFalse(level.getName().contains("Siêu khó"));
        }
    }

    @Test
    @DisplayName("Harder levels give less sun and send more waves and more zombies")
    void testDifficultyRises() {
        assertTrue(easy.getStartingSun() > medium.getStartingSun());
        assertTrue(medium.getStartingSun() > hard.getStartingSun());

        assertTrue(easy.getWaves().size() < medium.getWaves().size());
        assertTrue(medium.getWaves().size() < hard.getWaves().size());

        assertTrue(zombieCount(easy) < zombieCount(medium));
        assertTrue(zombieCount(medium) < zombieCount(hard));
    }

    @Test
    @DisplayName("Tougher zombie types are introduced step by step")
    void testZombieTypesPerLevel() {
        assertFalse(usesZombie(easy, "POLE_VAULTING"));
        assertFalse(usesZombie(easy, "BUCKETHEAD"));
        assertTrue(usesZombie(medium, "POLE_VAULTING"));
        assertTrue(usesZombie(medium, "BUCKETHEAD"));
        assertFalse(usesZombie(medium, "WATER"));
        assertTrue(usesZombie(hard, "WATER"));
    }

    @Test
    @DisplayName("Terrain: easy is open grass, medium has craters, hard is the pool")
    void testTerrain() {
        assertEquals(GameBoard.ROWS * GameBoard.COLS, count(easy, TileType.GRASS));
        assertEquals(4, count(medium, TileType.CRATER));
        assertEquals(0, count(medium, TileType.WATER));
        assertEquals(2 * GameBoard.COLS, count(hard, TileType.WATER));
        assertTrue(hard.getAllowedPlants().contains("LILY_PAD"));
    }

    @Test
    @DisplayName("Water zombies only spawn on water lanes")
    void testWaterZombiesOnWaterLanes() {
        for (GameMap level : levels()) {
            for (WaveConfig wave : level.getWaves()) {
                for (SpawnConfig spawn : wave.getSpawns()) {
                    if (spawn.getZombieType().equals("WATER")) {
                        assertEquals(TileType.WATER, level.getTileType(spawn.getLane(), 0));
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("Every level ends on a huge wave, and every huge wave starts with a flag")
    void testHugeWaves() {
        for (GameMap level : levels()) {
            List<WaveConfig> waves = level.getWaves();
            assertTrue(waves.get(waves.size() - 1).isHugeWave(), level.getName());
            for (WaveConfig wave : waves) {
                if (wave.isHugeWave()) {
                    assertEquals("FLAG", wave.getSpawns().get(0).getZombieType());
                }
            }
        }
    }

    @Test
    @DisplayName("Each call builds a separate map")
    void testIndependentBuilds() {
        GameMap again = director.buildEasyLevel(newBuilder());
        assertNotSame(easy, again);
        assertEquals(easy.getWaves().size(), again.getWaves().size());
    }

    @Test
    @DisplayName("Templates set terrain only: day is all grass, pool has two water rows")
    void testTemplateTerrain() {
        GameMap day = director.buildDayTemplate(newBuilder());
        GameMap pool = director.buildPoolTemplate(newBuilder());

        assertEquals(GameBoard.ROWS * GameBoard.COLS, count(day, TileType.GRASS));
        assertEquals(2 * GameBoard.COLS, count(pool, TileType.WATER));
        assertEquals(0, count(pool, TileType.CRATER));
        for (int c = 0; c < GameBoard.COLS; c++) {
            assertEquals(TileType.WATER, pool.getTileType(2, c));
            assertEquals(TileType.WATER, pool.getTileType(3, c));
        }
        for (GameMap template : List.of(day, pool)) {
            assertEquals(MapMode.CUSTOM, template.getMode());
            assertTrue(template.getAllowedPlants().isEmpty(), "Templates pick no plants");
            assertEquals("", template.getName());
        }
    }

    @Test
    @DisplayName("A template keeps the other settings already on the builder")
    void testTemplateKeepsSettings() {
        MapBuilder builder = newBuilder()
                .setName("Ho cua toi")
                .setStartingSun(200)
                .setAllowedPlants(List.of("PEASHOOTER"))
                .setMaxTurns(4)
                .setTile(0, 0, TileType.CRATER);

        GameMap map = director.buildPoolTemplate(builder);

        assertEquals("Ho cua toi", map.getName());
        assertEquals(200, map.getStartingSun());
        assertEquals(List.of("PEASHOOTER"), map.getAllowedPlants());
        assertEquals(4, map.getMaxTurns());
        assertEquals(TileType.GRASS, map.getTileType(0, 0), "Old terrain is replaced");
    }

    private static int zombieCount(GameMap map) {
        return map.getWaves().stream().mapToInt(w -> w.getSpawns().size()).sum();
    }

    private static boolean usesZombie(GameMap map, String type) {
        return map.getWaves().stream()
                .flatMap(w -> w.getSpawns().stream())
                .anyMatch(s -> s.getZombieType().equals(type));
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
