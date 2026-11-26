package com.pvz.builder;

import java.util.ArrayList;
import java.util.List;

import com.pvz.model.board.GameBoard;
import com.pvz.model.tile.TileType;

/**
 * Director of the Builder pattern: knows the steps that make each built-in map and runs
 * them on any {@link MapBuilder}.
 * <ul>
 * <li>Campaign: three levels, easy, medium and hard. Difficulty comes from less starting
 * sun, more and tougher zombies, shorter gaps between them, and terrain that leaves
 * fewer places to plant.</li>
 * <li>Custom: two terrain templates for the map editor, "Ban ngày" and "Bể bơi". They set
 * terrain only; the player picks everything else.</li>
 * </ul>
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

    // ---- Campaign ----

    /**
     * Easy: open grass, 150 sun and only the three basic plants. Three waves of mostly
     * Normal zombies with long gaps, so a new player can learn to build an economy.
     */
    public GameMap buildEasyLevel(MapBuilder builder) {
        builder.setName("Ban ngày (Dễ)")
                .setStartingSun(150)
                .setAllowedPlants(List.of(SUNFLOWER, PEASHOOTER, WALL_NUT));

        // Wave 1: one zombie at a time, the first only after 20 s
        builder.addWave(wave(false,
                at("NORMAL", 20.0, 2),
                at("NORMAL", 35.0, 1),
                at("NORMAL", 45.0, 3)));

        // Wave 2: the first Conehead
        builder.addWave(wave(false,
                at("NORMAL", 10.0, 0),
                at("NORMAL", 18.0, 4),
                at("CONEHEAD", 26.0, 2),
                at("NORMAL", 34.0, 1)));

        // Wave 3 (huge): a small rush to finish
        builder.addWave(wave(true,
                at("FLAG", 0.0, 2),
                at("NORMAL", 3.0, 1, 3),
                at("CONEHEAD", 8.0, 0, 4),
                at("NORMAL", 12.0, 2)));

        return builder.build();
    }

    /**
     * Medium: grass with four craters, 100 sun, Snow Pea and Cherry Bomb unlocked. Four
     * waves introduce Pole Vaulting and Buckethead zombies.
     */
    public GameMap buildMediumLevel(MapBuilder builder) {
        builder.setName("Sân hố (Trung bình)")
                .setStartingSun(100)
                .setAllowedPlants(List.of(SUNFLOWER, PEASHOOTER, SNOW_PEA, WALL_NUT,
                        CHERRY_BOMB));

        // Craters in columns 3 and 6 break up the usual planting rows
        builder.setTile(1, 3, TileType.CRATER)
                .setTile(3, 3, TileType.CRATER)
                .setTile(0, 6, TileType.CRATER)
                .setTile(4, 6, TileType.CRATER);

        // Wave 1
        builder.addWave(wave(false,
                at("NORMAL", 15.0, 2),
                at("NORMAL", 25.0, 0, 4),
                at("CONEHEAD", 35.0, 1)));

        // Wave 2: the first vaulter jumps the first plant in its lane
        builder.addWave(wave(false,
                at("CONEHEAD", 6.0, 3),
                at("NORMAL", 12.0, 1, 2),
                at("POLE_VAULTING", 20.0, 0),
                at("NORMAL", 26.0, 4)));

        // Wave 3 (huge)
        builder.addWave(wave(true,
                at("FLAG", 0.0, 2),
                at("NORMAL", 3.0, 0, 1, 3, 4),
                at("CONEHEAD", 6.0, 2),
                at("POLE_VAULTING", 10.0, 1)));

        // Wave 4 (huge, final): the first Buckethead
        builder.addWave(wave(true,
                at("FLAG", 0.0, 1),
                at("CONEHEAD", 3.0, 0, 2, 4),
                at("BUCKETHEAD", 8.0, 3),
                at("POLE_VAULTING", 11.0, 1, 3),
                at("NORMAL", 14.0, 0, 4)));

        return builder.build();
    }

    /**
     * Hard: the pool, two craters and only 50 sun, with every plant. Five waves send
     * swimmers from the start and armor on every lane at the end.
     */
    public GameMap buildHardLevel(MapBuilder builder) {
        builder.setName("Bể bơi (Khó)")
                .setStartingSun(50)
                .setAllowedPlants(List.of(SUNFLOWER, PEASHOOTER, SNOW_PEA, WALL_NUT,
                        CHERRY_BOMB, LILY_PAD));

        paintPoolRows(builder);
        builder.setTile(1, 5, TileType.CRATER)
                .setTile(4, 2, TileType.CRATER);

        // Wave 1: swimmers before the player can afford many LilyPads
        builder.addWave(wave(false,
                at("WATER", 10.0, 2),
                at("NORMAL", 16.0, 0),
                at("WATER", 22.0, 3),
                at("NORMAL", 28.0, 4)));

        // Wave 2
        builder.addWave(wave(false,
                at("CONEHEAD", 4.0, 0),
                at("WATER", 8.0, 2, 3),
                at("NORMAL", 12.0, 1),
                at("POLE_VAULTING", 18.0, 4)));

        // Wave 3 (huge)
        builder.addWave(wave(true,
                at("FLAG", 0.0, 1),
                at("WATER", 2.0, 2, 3),
                at("CONEHEAD", 4.0, 0, 4),
                at("NORMAL", 6.0, 1),
                at("POLE_VAULTING", 9.0, 0)));

        // Wave 4: a land zombie sent into the water; it swims or drowns
        builder.addWave(wave(false,
                at("BUCKETHEAD", 3.0, 1),
                at("WATER", 5.0, 2, 3),
                at("CONEHEAD", 8.0, 2),
                at("POLE_VAULTING", 10.0, 4),
                at("NORMAL", 12.0, 3)));

        // Wave 5 (huge, final): armor on every land lane
        builder.addWave(wave(true,
                at("FLAG", 0.0, 0),
                at("WATER", 1.0, 2, 3),
                at("BUCKETHEAD", 3.0, 0, 4),
                at("CONEHEAD", 5.0, 1, 2, 3),
                at("POLE_VAULTING", 8.0, 1, 4),
                at("WATER", 10.0, 2, 3)));

        return builder.build();
    }

    // ---- Custom terrain templates ----

    /**
     * Editor template "Ban ngày": an all-grass board. Sets terrain only; name, sun,
     * plants and turns stay as they are on the builder.
     */
    public GameMap buildDayTemplate(MapBuilder builder) {
        for (int r = 0; r < GameBoard.ROWS; r++) {
            for (int c = 0; c < GameBoard.COLS; c++) {
                builder.setTile(r, c, TileType.GRASS);
            }
        }
        return builder.build();
    }

    /**
     * Editor template "Bể bơi": grass with water on rows 2 and 3. Sets terrain only, like
     * {@link #buildDayTemplate(MapBuilder)}.
     */
    public GameMap buildPoolTemplate(MapBuilder builder) {
        buildDayTemplate(builder);
        paintPoolRows(builder);
        return builder.build();
    }

    /** Rows 2 and 3 become water; shared by the hard level and the pool template. */
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
