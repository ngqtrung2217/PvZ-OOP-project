package com.pvz.model.zombie;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pvz.model.board.GameBoard;
import javafx.scene.paint.Color;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Factory class responsible for creating and configuring Zombie instances.
 * Reads specifications from /data/zombies_spec.json with safe fallbacks,
 * and supports deterministic {@link Random} injection for swim/drown rolls.
 */
public final class ZombieFactory {

    public static class ZombieSpec {
        public String type;
        public int maxHp;
        public int armorHp;
        public double speed;
        public double swimSpeed;
        public int attackDamage;
        public double width;
        public double height;
        public String colorHex;
        public double drownChance;

        public ZombieSpec() {}

        public ZombieSpec(String type, int maxHp, int armorHp, double speed,
                          double swimSpeed, int attackDamage, double width,
                          double height, String colorHex) {
            this(type, maxHp, armorHp, speed, swimSpeed, attackDamage, width, height, colorHex,
                    "WATER".equalsIgnoreCase(type) ? 0.0 : 0.8);
        }

        public ZombieSpec(String type, int maxHp, int armorHp, double speed,
                          double swimSpeed, int attackDamage, double width,
                          double height, String colorHex, double drownChance) {
            this.type = type;
            this.maxHp = maxHp;
            this.armorHp = armorHp;
            this.speed = speed;
            this.swimSpeed = swimSpeed;
            this.attackDamage = attackDamage;
            this.width = width;
            this.height = height;
            this.colorHex = colorHex;
            this.drownChance = drownChance;
        }
    }

    private static final Map<String, ZombieSpec> SPECS = new HashMap<>();

    static {
        loadSpecs();
    }

    private final Random random;

    public ZombieFactory() {
        this(new Random());
    }

    public ZombieFactory(Random random) {
        this.random = random != null ? random : new Random();
    }

    /**
     * Loads zombie specifications from JSON configuration or applies defaults.
     */
    private static void loadSpecs() {
        try (InputStream is = ZombieFactory.class.getResourceAsStream("/data/zombies_spec.json")) {
            if (is != null) {
                try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    Type listType = new TypeToken<List<ZombieSpec>>() {}.getType();
                    List<ZombieSpec> specList = new Gson().fromJson(reader, listType);
                    if (specList != null) {
                        for (ZombieSpec spec : specList) {
                            if (spec.type != null) {
                                SPECS.put(spec.type.toUpperCase(), spec);
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // Fallback to hardcoded defaults
        }

        initDefaultSpecs();
    }

    private static void initDefaultSpecs() {
        SPECS.putIfAbsent("NORMAL", new ZombieSpec("NORMAL", 200, 0, 17.0, 17.0, 25, 50.0, 90.0, "#4A7C59", 0.8));
        SPECS.putIfAbsent("FLAG", new ZombieSpec("FLAG", 200, 0, 22.0, 22.0, 25, 50.0, 90.0, "#5A8C69", 0.7));
        SPECS.putIfAbsent("CONEHEAD", new ZombieSpec("CONEHEAD", 200, 370, 17.0, 17.0, 25, 50.0, 90.0, "#4A7C59", 0.85));
        SPECS.putIfAbsent("BUCKETHEAD", new ZombieSpec("BUCKETHEAD", 200, 1100, 17.0, 17.0, 25, 50.0, 90.0, "#4A7C59", 0.95));
        SPECS.putIfAbsent("POLE_VAULTING", new ZombieSpec("POLE_VAULTING", 500, 0, 31.0, 17.0, 25, 50.0, 90.0, "#3D6647", 0.5));
        SPECS.putIfAbsent("WATER", new ZombieSpec("WATER", 200, 0, 17.0, 22.0, 25, 50.0, 90.0, "#2E5E4E", 0.0));
    }

    /**
     * Instance method using the injected {@link Random} to spawn a configured Zombie.
     */
    public Zombie create(String type, int lane, GameBoard board) {
        return createZombie(type, lane, board, this.random);
    }

    /**
     * Spawns a Zombie of the given type at the starting right edge of the board on the specified lane.
     *
     * @param type  Zombie type string (e.g., "NORMAL", "FLAG", "CONEHEAD", "BUCKETHEAD", "POLE_VAULTING", "WATER").
     * @param lane  Lane index (0 to 4).
     * @param board The GameBoard instance, or null in isolated testing.
     * @return Configured Zombie instance.
     * @throws IllegalArgumentException if type is null or unrecognized.
     */
    public static Zombie createZombie(String type, int lane, GameBoard board) {
        return createZombie(type, lane, board, new Random());
    }

    /**
     * Spawns a Zombie using an explicit {@link Random} instance for deterministic drown/swim rolls.
     */
    public static Zombie createZombie(String type, int lane, GameBoard board, Random random) {
        if (type == null) {
            throw new IllegalArgumentException("Zombie type cannot be null");
        }

        String normalizedType = type.trim().toUpperCase();
        ZombieSpec spec = SPECS.get(normalizedType);
        if (spec == null) {
            throw new IllegalArgumentException("Unknown zombie type: " + type);
        }

        double height = spec.height > 0 ? spec.height : 90.0;
        double x = GameBoard.START_X + GameBoard.COLS * GameBoard.CELL_WIDTH;
        double y = board != null ? board.getCellOriginY(lane) + (GameBoard.CELL_HEIGHT - height) : 100.0 + lane * 100.0;

        Zombie zombie;
        switch (normalizedType) {
            case "NORMAL":
                zombie = new NormalZombie(x, y, lane, board);
                break;
            case "FLAG":
                zombie = new FlagZombie(x, y, lane, board);
                break;
            case "CONEHEAD":
                zombie = new ConeheadZombie(x, y, lane, board);
                if (spec.armorHp > 0) {
                    ((ConeheadZombie) zombie).setArmorHp(spec.armorHp);
                }
                break;
            case "BUCKETHEAD":
                zombie = new BucketheadZombie(x, y, lane, board);
                if (spec.armorHp > 0) {
                    ((BucketheadZombie) zombie).setArmorHp(spec.armorHp);
                }
                break;
            case "POLE_VAULTING":
                zombie = new PoleVaultingZombie(x, y, lane, board);
                break;
            case "WATER":
                zombie = new WaterZombie(x, y, lane, board);
                break;
            default:
                throw new IllegalArgumentException("Unknown zombie type: " + type);
        }

        if (spec.speed > 0) {
            zombie.setSpeed(spec.speed);
        }
        if (spec.attackDamage > 0) {
            zombie.setAttackDamage(spec.attackDamage);
        }
        if (spec.swimSpeed > 0) {
            zombie.setSwimSpeed(spec.swimSpeed);
        }
        if (spec.colorHex != null && !spec.colorHex.isEmpty()) {
            try {
                zombie.setRenderColor(Color.web(spec.colorHex));
            } catch (Exception ignored) {}
        }
        zombie.setBoard(board);
        zombie.setLane(lane);
        zombie.setType(normalizedType);

        // Roll drownChance once at spawn to decide canSwim
        if (zombie instanceof WaterZombie || "WATER".equals(normalizedType)) {
            zombie.setCanSwim(true);
        } else {
            Random rng = random != null ? random : new Random();
            double roll = rng.nextDouble();
            zombie.setCanSwim(roll >= spec.drownChance);
        }

        return zombie;
    }

    public static ZombieSpec getSpec(String type) {
        if (type == null) {
            return null;
        }
        return SPECS.get(type.trim().toUpperCase());
    }
}
