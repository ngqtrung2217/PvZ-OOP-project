package com.pvz.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.pvz.core.GameManager;
import com.pvz.model.board.GameBoard;
import com.pvz.model.tile.CraterTile;
import com.pvz.model.tile.TileType;
import com.pvz.model.tile.WaterTile;
import com.pvz.model.wave.Wave;
import com.pvz.model.wave.WaveEntry;

/**
 * Unit tests for CustomMapBuilder validation and the GameMap it builds.
 */
class MapBuilderTest {
    private MapBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new CustomMapBuilder(Set.of("PLANT_A", "PLANT_B", "SUPPORT_A"));
    }

    @Test
    @DisplayName("Default map is unnamed, endless, grass, standard sun and no plants")
    void testDefaults() {
        GameMap map = builder.build();
        assertEquals("", map.getName());
        assertEquals(TileType.GRASS, map.getTileType(0, 0));
        assertEquals(GameManager.STARTING_SUN, map.getStartingSun());
        assertTrue(map.getAllowedPlants().isEmpty());
        assertNull(map.getMaxTurns());
        assertTrue(map.isEndless());
    }

    @Test
    @DisplayName("Name is trimmed, blank is allowed, null is rejected")
    void testName() {
        assertEquals("Ao giua san", builder.setName("  Ao giua san ").build().getName());
        assertEquals("", builder.setName("   ").build().getName());
        assertThrows(IllegalArgumentException.class, () -> builder.setName(null));
    }

    @Test
    @DisplayName("Allowed plants keep their order and drop duplicates")
    void testAllowedPlants() {
        GameMap map = builder.setAllowedPlants(List.of("PLANT_B", "PLANT_A", "PLANT_B")).build();
        assertEquals(List.of("PLANT_B", "PLANT_A"), map.getAllowedPlants());
    }

    @Test
    @DisplayName("Null lists and unknown plant IDs are rejected")
    void testInvalidAllowedPlants() {
        assertThrows(IllegalArgumentException.class, () -> builder.setAllowedPlants(null));
        assertThrows(IllegalArgumentException.class,
                () -> builder.setAllowedPlants(List.of("PLANT_A", "ABC")));
        assertThrows(IllegalArgumentException.class,
                () -> builder.setAllowedPlants(Arrays.asList("PLANT_A", null)));
        // A rejected call must not leave half of the list behind
        assertTrue(builder.build().getAllowedPlants().isEmpty());
    }

    @Test
    @DisplayName("Allowed plants cannot be changed through the source list or the map")
    void testAllowedPlantsAreSnapshot() {
        List<String> source = new ArrayList<>(List.of("PLANT_A"));
        GameMap map = builder.setAllowedPlants(source).build();

        source.add("PLANT_B");

        assertEquals(List.of("PLANT_A"), map.getAllowedPlants());
        assertThrows(UnsupportedOperationException.class,
                () -> map.getAllowedPlants().add("PLANT_B"));
    }

    @Test
    @DisplayName("A map without waves is custom, a map with waves is campaign")
    void testMode() {
        assertEquals(MapMode.CUSTOM, builder.setMaxTurns(5).build().getMode());

        GameMap campaign = new CustomMapBuilder(Set.of())
                .addWave(waveOf("NORMAL", 0, 1.0))
                .build();
        assertEquals(MapMode.CAMPAIGN, campaign.getMode());
        assertFalse(campaign.isEndless());
    }

    @Test
    @DisplayName("Scripted waves and a turn limit cannot be combined, in either order")
    void testWavesAndMaxTurnsConflict() {
        builder.setMaxTurns(3).addWave(waveOf("NORMAL", 0, 1.0));
        assertThrows(IllegalStateException.class, () -> builder.build());

        MapBuilder other = new CustomMapBuilder(Set.of())
                .addWave(waveOf("NORMAL", 0, 1.0))
                .setMaxTurns(3);
        assertThrows(IllegalStateException.class, () -> other.build());
    }

    @Test
    @DisplayName("Max turns accepts a positive count or null for endless")
    void testMaxTurns() {
        GameMap limited = builder.setMaxTurns(5).build();
        assertEquals(5, limited.getMaxTurns());
        assertFalse(limited.isEndless());

        assertTrue(builder.setMaxTurns(null).build().isEndless());

        assertThrows(IllegalArgumentException.class, () -> builder.setMaxTurns(0));
        assertThrows(IllegalArgumentException.class, () -> builder.setMaxTurns(-3));
    }

    @Test
    @DisplayName("Cells outside the grid are rejected")
    void testOutOfBounds() {
        assertThrows(IllegalArgumentException.class,
                () -> builder.setTile(-1, 0, TileType.GRASS));
        assertThrows(IllegalArgumentException.class,
                () -> builder.setTile(0, GameBoard.COLS, TileType.GRASS));
    }

    @Test
    @DisplayName("Chained steps end up in the built map")
    void testChaining() {
        GameMap map = builder.setName("Pool")
                .setTile(2, 3, TileType.WATER)
                .setStartingSun(200)
                .setAllowedPlants(List.of("PLANT_A", "SUPPORT_A"))
                .setMaxTurns(3)
                .build();

        assertEquals("Pool", map.getName());
        assertEquals(TileType.WATER, map.getTileType(2, 3));
        assertEquals(200, map.getStartingSun());
        assertEquals(List.of("PLANT_A", "SUPPORT_A"), map.getAllowedPlants());
        assertEquals(3, map.getMaxTurns());
    }

    @Test
    @DisplayName("Negative sun is rejected, zero is allowed")
    void testSunLimits() {
        assertThrows(IllegalArgumentException.class, () -> builder.setStartingSun(-1));
        assertEquals(0, builder.setStartingSun(0).build().getStartingSun());
    }

    @Test
    @DisplayName("createBoard puts each tile at the right position")
    void testCreateBoard() {
        GameMap map = builder.setTile(2, 3, TileType.WATER).build();
        GameBoard board = map.createBoard();

        assertInstanceOf(WaterTile.class, board.getTile(2, 3));
        for (int r = 0; r < GameBoard.ROWS; r++) {
            for (int c = 0; c < GameBoard.COLS; c++) {
                assertEquals(map.getTileType(r, c), board.getTile(r, c).getType());
                assertEquals(board.getCellOriginX(c), board.getTile(r, c).getX(), 0.001);
                assertEquals(board.getCellOriginY(r), board.getTile(r, c).getY(), 0.001);
            }
        }
    }

    @Test
    @DisplayName("Changing the builder does not affect a built map")
    void testBuiltMapIsSnapshot() {
        GameMap map = builder.build();

        builder.setTile(0, 0, TileType.WATER);

        assertEquals(TileType.GRASS, map.getTileType(0, 0));
    }

    @Test
    @DisplayName("Null tile type is rejected")
    void testNullTile() {
        assertThrows(IllegalArgumentException.class, () -> builder.setTile(0, 0, null));
    }

    @Test
    @DisplayName("Boards created from the same map are independent")
    void testBoardsAreIndependent() {
        GameMap map = builder.build();
        GameBoard a = map.createBoard();
        GameBoard b = map.createBoard();

        a.setTile(0, 0, new CraterTile(0, 0, a.getCellOriginX(0), a.getCellOriginY(0),
                GameBoard.CELL_WIDTH, GameBoard.CELL_HEIGHT));

        assertEquals(TileType.CRATER, a.getTile(0, 0).getType());
        assertEquals(TileType.GRASS, b.getTile(0, 0).getType());
        assertEquals(TileType.GRASS, map.getTileType(0, 0));
    }

    @Test
    @DisplayName("Added waves keep their order and content")
    void testAddWave() {
        GameMap map = builder.addWave(waveOf("NORMAL", 1, 2.0))
                .addWave(new WaveConfig(List.of(new SpawnConfig("FLAG", 4, 0)), true))
                .build();

        List<Wave> waves = map.createWaves();
        assertEquals(2, waves.size());
        assertFalse(waves.get(0).isHugeWave());
        assertTrue(waves.get(1).isHugeWave());

        WaveEntry entry = waves.get(0).getEntries().get(0);
        assertEquals("NORMAL", entry.getZombieType());
        assertEquals(1, entry.getLane());
        assertEquals(2.0, entry.getSpawnDelay(), 0.001);
        assertFalse(entry.isSpawned());
    }

    @Test
    @DisplayName("Playing one session's waves does not affect the next session")
    void testWavesAreIndependent() {
        GameMap map = builder.addWave(waveOf("NORMAL", 0, 1.0)).build();

        map.createWaves().get(0).getEntries().get(0).setSpawned(true);

        assertFalse(map.createWaves().get(0).getEntries().get(0).isSpawned());
    }

    @Test
    @DisplayName("Changing the builder or the source list does not affect built waves")
    void testWavesAreSnapshot() {
        List<SpawnConfig> spawns = new ArrayList<>();
        spawns.add(new SpawnConfig("NORMAL", 0, 1.0));
        GameMap map = builder.addWave(new WaveConfig(spawns, false)).build();

        spawns.clear();
        builder.addWave(waveOf("FLAG", 0, 1.0));

        assertEquals(1, map.getWaves().size());
        assertEquals(1, map.getWaves().get(0).getSpawns().size());
    }

    @Test
    @DisplayName("Null waves, invalid lanes and unknown zombie types are rejected")
    void testInvalidWave() {
        assertThrows(IllegalArgumentException.class, () -> builder.addWave(null));
        assertThrows(IllegalArgumentException.class,
                () -> builder.addWave(waveOf("NORMAL", -1, 1.0)));
        assertThrows(IllegalArgumentException.class,
                () -> builder.addWave(waveOf("NORMAL", GameBoard.ROWS, 1.0)));
        assertThrows(IllegalArgumentException.class,
                () -> builder.addWave(waveOf("ABC", 0, 1.0)));
        assertTrue(builder.build().getWaves().isEmpty());
    }

    @Test
    @DisplayName("Empty waves, blank types and impossible delays are rejected")
    void testInvalidWaveConfig() {
        assertThrows(IllegalArgumentException.class, () -> new WaveConfig(null, false));
        assertThrows(IllegalArgumentException.class, () -> new WaveConfig(List.of(), false));
        assertThrows(IllegalArgumentException.class, () -> new SpawnConfig(" ", 0, 1.0));
        assertThrows(IllegalArgumentException.class, () -> new SpawnConfig("NORMAL", 0, -1.0));
        assertThrows(IllegalArgumentException.class,
                () -> new SpawnConfig("NORMAL", 0, Double.NaN));
    }

    private static WaveConfig waveOf(String zombieType, int lane, double spawnDelay) {
        return new WaveConfig(List.of(new SpawnConfig(zombieType, lane, spawnDelay)), false);
    }
}
