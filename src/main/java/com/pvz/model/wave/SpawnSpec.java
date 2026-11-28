package com.pvz.model.wave;

import com.google.gson.Gson;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Configuration parameters for wave/survival spawning, loaded from /data/spawn_spec.json.
 */
public class SpawnSpec {
    public double gracePeriod = 5.0;
    public double T0 = 6.0;
    public double T_min = 1.2;
    public double decay = 0.92;
    public int maxActiveZombies = 20;
    public int zombiesPerTurn = 8;
    public List<String> spawnPool = Arrays.asList("NORMAL", "CONEHEAD", "BUCKETHEAD", "POLE_VAULTING", "WATER");

    public SpawnSpec() {}

    public SpawnSpec(double gracePeriod, double t0, double tMin, double decay,
                     int maxActiveZombies, int zombiesPerTurn, List<String> spawnPool) {
        this.gracePeriod = gracePeriod;
        this.T0 = t0;
        this.T_min = tMin;
        this.decay = decay;
        this.maxActiveZombies = maxActiveZombies;
        this.zombiesPerTurn = zombiesPerTurn;
        if (spawnPool != null && !spawnPool.isEmpty()) {
            this.spawnPool = spawnPool;
        }
    }

    public static SpawnSpec load() {
        try (InputStream is = SpawnSpec.class.getResourceAsStream("/data/spawn_spec.json")) {
            if (is != null) {
                try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    SpawnSpec loaded = new Gson().fromJson(reader, SpawnSpec.class);
                    if (loaded != null) {
                        if (loaded.spawnPool == null || loaded.spawnPool.isEmpty()) {
                            loaded.spawnPool = Arrays.asList("NORMAL", "CONEHEAD", "BUCKETHEAD", "POLE_VAULTING", "WATER");
                        }
                        if (loaded.zombiesPerTurn <= 0) {
                            loaded.zombiesPerTurn = 8;
                        }
                        return loaded;
                    }
                }
            }
        } catch (Exception ignored) {
            // Fall back to default values
        }
        return new SpawnSpec();
    }

    public double getGracePeriod() {
        return gracePeriod;
    }

    public double getT0() {
        return T0;
    }

    public double getT_min() {
        return T_min;
    }

    public double getDecay() {
        return decay;
    }

    public int getMaxActiveZombies() {
        return maxActiveZombies;
    }

    public int getZombiesPerTurn() {
        return zombiesPerTurn;
    }

    public List<String> getSpawnPool() {
        return Collections.unmodifiableList(spawnPool);
    }
}
