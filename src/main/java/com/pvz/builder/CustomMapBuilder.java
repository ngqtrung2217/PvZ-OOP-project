package com.pvz.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.pvz.core.GameManager;
import com.pvz.model.board.GameBoard;
import com.pvz.model.tile.TileType;
import com.pvz.model.zombie.ZombieFactory;

/**
 * Default {@link MapBuilder}. Starts from an unnamed, endless, all-grass board with the
 * standard sun balance and no plants.
 */
public class CustomMapBuilder implements MapBuilder {
    private final TileType[][] tiles = new TileType[GameBoard.ROWS][GameBoard.COLS];
    private final List<WaveConfig> waves = new ArrayList<>();
    private final Set<String> validPlantIds;
    private String name = "";
    private int startingSun = GameManager.STARTING_SUN;
    private List<String> allowedPlants = List.of();
    private Integer maxTurns = null;

    /**
     * @param validPlantIds every plant ID the game knows.
     *                      TODO(An Nguyen): switch to PlantType once model/plant exists.
     */
    public CustomMapBuilder(Set<String> validPlantIds) {
        this.validPlantIds = Set.copyOf(validPlantIds);
        for (int r = 0; r < GameBoard.ROWS; r++) {
            Arrays.fill(tiles[r], TileType.GRASS);
        }
    }

    @Override
    public MapBuilder setName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        this.name = name.strip();
        return this;
    }

    @Override
    public MapBuilder setTile(int row, int col, TileType type) {
        requireValidCell(row, col);
        if (type == null) {
            throw new IllegalArgumentException("Tile type cannot be null");
        }
        tiles[row][col] = type;
        return this;
    }

    @Override
    public MapBuilder setStartingSun(int sun) {
        if (sun < 0) {
            throw new IllegalArgumentException("Starting sun cannot be negative");
        }
        this.startingSun = sun;
        return this;
    }

    @Override
    public MapBuilder setAllowedPlants(List<String> plantIds) {
        if (plantIds == null) {
            throw new IllegalArgumentException("Plant list cannot be null");
        }
        for (String id : plantIds) {
            if (!validPlantIds.contains(id)) {
                throw new IllegalArgumentException("Unknown plant ID: " + id);
            }
        }
        // LinkedHashSet drops duplicates but keeps the seed-bar order
        this.allowedPlants = List.copyOf(new LinkedHashSet<>(plantIds));
        return this;
    }

    @Override
    public MapBuilder setMaxTurns(Integer turns) {
        if (turns != null && turns < 1) {
            throw new IllegalArgumentException("Max turns must be at least 1, or null for endless");
        }
        this.maxTurns = turns;
        return this;
    }

    @Override
    public MapBuilder addWave(WaveConfig wave) {
        if (wave == null) {
            throw new IllegalArgumentException("Wave cannot be null");
        }
        for (SpawnConfig spawn : wave.getSpawns()) {
            requireValidSpawn(spawn);
        }
        waves.add(wave);
        return this;
    }

    /**
     * Checked here rather than in the setters because the two can be set in either order.
     *
     * @throws IllegalStateException if both scripted waves and a turn limit are set
     */
    @Override
    public GameMap build() {
        if (!waves.isEmpty() && maxTurns != null) {
            throw new IllegalStateException(
                    "A campaign map ends after its waves and cannot also set max turns");
        }
        return new GameMap(name, tiles, startingSun, allowedPlants, maxTurns, waves);
    }

    /** Otherwise a bad spawn would only show up in the middle of a game. */
    private static void requireValidSpawn(SpawnConfig spawn) {
        if (spawn.getLane() < 0 || spawn.getLane() >= GameBoard.ROWS) {
            throw new IllegalArgumentException("Lane out of bounds: " + spawn.getLane());
        }
        if (ZombieFactory.getSpec(spawn.getZombieType()) == null) {
            throw new IllegalArgumentException("Unknown zombie type: " + spawn.getZombieType());
        }
    }

    private static void requireValidCell(int row, int col) {
        if (row < 0 || row >= GameBoard.ROWS || col < 0 || col >= GameBoard.COLS) {
            throw new IllegalArgumentException("Cell out of bounds: (" + row + ", " + col + ")");
        }
    }
}
