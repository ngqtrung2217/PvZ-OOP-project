package com.pvz.builder;

import java.util.ArrayList;
import java.util.List;

import com.pvz.model.board.GameBoard;
import com.pvz.model.tile.TileType;

/**
 * Director of the Builder pattern: knows the steps that make each built-in map and runs
 * them on any {@link MapBuilder}. Makes the two campaign levels, tuned to be very hard,
 * and the two quick templates of the map editor.
 * <p>
 * Pass a fresh builder to each method, because a builder keeps everything set on it.
 */
public final class LevelDirector {
    // TODO(An Nguyen): switch to PlantType constants once model/plant exists
    private static final String PEASHOOTER = "PEASHOOTER";
    private static final String SNOW_PEA = "SNOW_PEA";
    private static final String SUNFLOWER = "SUNFLOWER";
    private static final String WALL_NUT = "WALL_NUT";
    private static final String CHERRY_BOMB = "CHERRY_BOMB";
    private static final String LILY_PAD = "LILY_PAD";

    /** Every plant ID the built-in levels use; the builder passed in must accept them. */
    public static final List<String> PLANTS_USED =
            List.of(PEASHOOTER, SNOW_PEA, SUNFLOWER, WALL_NUT, CHERRY_BOMB, LILY_PAD);

    private static final int HARD_STARTING_SUN = 50;

    /**
     * Day level: grass with craters that break up the planting columns. No Snow Pea, so
     * nothing slows the zombies down. Five waves; waves 3 and 5 are huge waves.
     */
    public GameMap buildDayLevel(MapBuilder builder) {
        builder.setName("Ban ngày (Siêu khó)")
                .setStartingSun(HARD_STARTING_SUN)
                .setAllowedPlants(List.of(SUNFLOWER, PEASHOOTER, WALL_NUT, CHERRY_BOMB));

        // Craters in column 2 and 5 take away the usual second and third shooter columns
        builder.setTile(0, 2, TileType.CRATER)
                .setTile(2, 2, TileType.CRATER)
                .setTile(4, 2, TileType.CRATER)
                .setTile(1, 5, TileType.CRATER)
                .setTile(3, 5, TileType.CRATER);

        // Wave 1: a fast opener on every lane before the economy is running
        builder.addWave(wave(false,
                at("NORMAL", 8.0, 2),
                at("NORMAL", 12.0, 0, 4),
                at("NORMAL", 16.0, 1, 3),
                at("CONEHEAD", 20.0, 2)));

        // Wave 2: armor and the first vaulters, which jump the first Wall-nut
        builder.addWave(wave(false,
                at("CONEHEAD", 2.0, 1, 3),
                at("POLE_VAULTING", 6.0, 0, 4),
                at("NORMAL", 9.0, 2),
                at("CONEHEAD", 12.0, 0, 2, 4)));

        // Wave 3 (huge): many zombies arrive together on the middle lanes
        builder.addWave(wave(true,
                at("FLAG", 0.0, 2),
                at("CONEHEAD", 1.0, 1, 2, 3),
                at("NORMAL", 2.0, 0, 1, 2, 3, 4),
                at("POLE_VAULTING", 4.0, 1, 3),
                at("BUCKETHEAD", 7.0, 2)));

        // Wave 4: heavy armor on the edge lanes, vaulters in the middle
        builder.addWave(wave(false,
                at("BUCKETHEAD", 2.0, 0, 4),
                at("POLE_VAULTING", 4.0, 1, 2, 3),
                at("CONEHEAD", 7.0, 0, 1, 3, 4),
                at("BUCKETHEAD", 11.0, 2)));

        // Wave 5 (huge, final): every lane gets armor at once
        builder.addWave(wave(true,
                at("FLAG", 0.0, 2),
                at("BUCKETHEAD", 1.0, 0, 2, 4),
                at("CONEHEAD", 2.0, 0, 1, 2, 3, 4),
                at("POLE_VAULTING", 3.0, 1, 3),
                at("NORMAL", 4.0, 0, 1, 2, 3, 4),
                at("BUCKETHEAD", 6.0, 1, 3),
                at("POLE_VAULTING", 8.0, 0, 4)));

        return builder.build();
    }

    /**
     * Pool level: rows 2 and 3 are water, so a third of the board needs a LilyPad first.
     * Craters on the land rows force more plants onto the water. Six waves; waves 3 and 6
     * are huge waves.
     */
    public GameMap buildPoolLevel(MapBuilder builder) {
        builder.setName("Bể bơi (Siêu khó)")
                .setStartingSun(HARD_STARTING_SUN)
                .setAllowedPlants(List.of(SUNFLOWER, PEASHOOTER, SNOW_PEA, WALL_NUT,
                        CHERRY_BOMB, LILY_PAD));

        paintPoolRows(builder);
        builder.setTile(0, 1, TileType.CRATER)
                .setTile(1, 4, TileType.CRATER)
                .setTile(4, 1, TileType.CRATER)
                .setTile(4, 4, TileType.CRATER);

        // Wave 1: swimmers right away, before the player can afford LilyPads
        builder.addWave(wave(false,
                at("WATER", 6.0, 2),
                at("NORMAL", 9.0, 0, 4),
                at("WATER", 12.0, 3),
                at("NORMAL", 15.0, 1)));

        // Wave 2: armor on land while the water lanes keep coming
        builder.addWave(wave(false,
                at("CONEHEAD", 2.0, 0, 4),
                at("WATER", 4.0, 2, 3),
                at("POLE_VAULTING", 7.0, 1),
                at("WATER", 10.0, 2, 3)));

        // Wave 3 (huge)
        builder.addWave(wave(true,
                at("FLAG", 0.0, 1),
                at("WATER", 1.0, 2, 3),
                at("CONEHEAD", 2.0, 0, 1, 4),
                at("WATER", 4.0, 2, 3),
                at("POLE_VAULTING", 5.0, 0, 4),
                at("NORMAL", 6.0, 1, 4)));

        // Wave 4: buckets on land, so the Cherry Bomb is needed in two places at once
        builder.addWave(wave(false,
                at("BUCKETHEAD", 2.0, 0, 4),
                at("WATER", 3.0, 2, 3),
                at("CONEHEAD", 6.0, 1),
                at("WATER", 8.0, 2, 3),
                at("POLE_VAULTING", 10.0, 1, 4)));

        // Wave 5: land zombies sent into the water lanes; some swim, some drown
        builder.addWave(wave(false,
                at("CONEHEAD", 2.0, 2, 3),
                at("BUCKETHEAD", 4.0, 1),
                at("WATER", 5.0, 2, 3),
                at("POLE_VAULTING", 7.0, 0, 4),
                at("NORMAL", 9.0, 2, 3)));

        // Wave 6 (huge, final): every lane, armor everywhere
        builder.addWave(wave(true,
                at("FLAG", 0.0, 0),
                at("WATER", 1.0, 2, 3),
                at("BUCKETHEAD", 2.0, 0, 1, 4),
                at("CONEHEAD", 3.0, 0, 1, 2, 3, 4),
                at("WATER", 5.0, 2, 3),
                at("POLE_VAULTING", 6.0, 0, 1, 4),
                at("BUCKETHEAD", 8.0, 1, 4),
                at("WATER", 9.0, 2, 3)));

        return builder.build();
    }

    /**
     * Editor quick template "Sân cỏ": an all-grass custom map with every land plant.
     * The name is left blank for the player to fill in, and the map is endless.
     * The builder can still be edited after this call.
     */
    public GameMap buildLawnTemplate(MapBuilder builder) {
        builder.setAllowedPlants(List.of(SUNFLOWER, PEASHOOTER, SNOW_PEA, WALL_NUT,
                CHERRY_BOMB));
        return builder.build();
    }

    /**
     * Editor quick template "Bể bơi": a custom map with water on rows 2 and 3 and every
     * plant, including LilyPad. The name is left blank and the map is endless.
     * The builder can still be edited after this call.
     */
    public GameMap buildPoolTemplate(MapBuilder builder) {
        paintPoolRows(builder);
        builder.setAllowedPlants(List.of(SUNFLOWER, PEASHOOTER, SNOW_PEA, WALL_NUT,
                CHERRY_BOMB, LILY_PAD));
        return builder.build();
    }

    /** Rows 2 and 3 become water; shared by the pool level and the pool template. */
    private static void paintPoolRows(MapBuilder builder) {
        for (int c = 0; c < GameBoard.COLS; c++) {
            builder.setTile(2, c, TileType.WATER)
                    .setTile(3, c, TileType.WATER);
        }
    }

    /** One zombie of the given type on each listed lane, all at the same delay. */
    private static List<SpawnConfig> at(String zombieType, double delay, int... lanes) {
        List<SpawnConfig> spawns = new ArrayList<>();
        for (int lane : lanes) {
            spawns.add(new SpawnConfig(zombieType, lane, delay));
        }
        return spawns;
    }

    @SafeVarargs
    private static WaveConfig wave(boolean huge, List<SpawnConfig>... groups) {
        List<SpawnConfig> spawns = new ArrayList<>();
        for (List<SpawnConfig> group : groups) {
            spawns.addAll(group);
        }
        return new WaveConfig(spawns, huge);
    }
}
