package com.pvz.model.wave;

import com.pvz.model.board.GameBoard;
import com.pvz.model.zombie.Zombie;
import com.pvz.model.zombie.ZombieFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Custom/Survival spawn strategy with escalating spawn frequency.
 * <p>
 * After an initial {@code gracePeriod}, spawns zombies continuously without waiting
 * for earlier waves to die out. The delay after the n-th spawn follows:
 * {@code interval(n) = Math.max(T_min, T0 * Math.pow(decay, n))}.
 * <p>
 * Respects {@code maxActiveZombies}, leads each turn with a {@code FLAG} zombie,
 * and supports both finite ({@code maxTurns != null}) and endless ({@code maxTurns == null}) modes.
 */
public class EscalatingRandomSpawnPolicy implements SpawnPolicy {
    private final SpawnSpec spec;
    private final Integer maxTurns;
    private final Random random;

    private double totalElapsedTime;
    private double graceRemaining;
    private double timeUntilNextSpawn;
    private int totalSpawnCount;
    private int currentTurnIndex;
    private int spawnedInCurrentTurn;
    private boolean hugeWaveFlag;

    public EscalatingRandomSpawnPolicy(Random random) {
        this(SpawnSpec.load(), null, random);
    }

    public EscalatingRandomSpawnPolicy(Integer maxTurns, Random random) {
        this(SpawnSpec.load(), maxTurns, random);
    }

    public EscalatingRandomSpawnPolicy(SpawnSpec spec, Integer maxTurns, Random random) {
        this.spec = spec != null ? spec : SpawnSpec.load();
        if (maxTurns != null && maxTurns <= 0) {
            throw new IllegalArgumentException("maxTurns must be positive when specified");
        }
        this.maxTurns = maxTurns;
        this.random = random != null ? random : new Random();
        this.totalElapsedTime = 0.0;
        this.graceRemaining = Math.max(0.0, this.spec.getGracePeriod());
        this.timeUntilNextSpawn = 0.0;
        this.totalSpawnCount = 0;
        this.currentTurnIndex = 0;
        this.spawnedInCurrentTurn = 0;
        this.hugeWaveFlag = false;
    }

    /**
     * Computes the spawn interval after the n-th spawn:
     * {@code Math.max(T_min, T0 * Math.pow(decay, n))}.
     */
    public double computeInterval(int n) {
        return Math.max(spec.getT_min(), spec.getT0() * Math.pow(spec.getDecay(), Math.max(0, n)));
    }

    @Override
    public List<Zombie> update(double deltaTime, GameBoard board, List<Zombie> activeZombies, ZombieFactory factory) {
        List<Zombie> spawned = new ArrayList<>();
        if (deltaTime <= 0.0) {
            return spawned;
        }
        totalElapsedTime += deltaTime;

        if (maxTurns != null && currentTurnIndex >= maxTurns) {
            return spawned;
        }

        double effectiveDelta = deltaTime;
        if (graceRemaining > 0.0) {
            if (effectiveDelta < graceRemaining) {
                graceRemaining -= effectiveDelta;
                return spawned;
            }
            effectiveDelta -= graceRemaining;
            graceRemaining = 0.0;
        }

        timeUntilNextSpawn -= effectiveDelta;

        int currentActive = activeZombies != null ? activeZombies.size() : 0;
        while (timeUntilNextSpawn <= 0.0) {
            if (maxTurns != null && currentTurnIndex >= maxTurns) {
                break;
            }
            if (currentActive + spawned.size() >= spec.getMaxActiveZombies()) {
                // Cap reached: hold timer at 0 so the next slot opens immediately on death
                timeUntilNextSpawn = 0.0;
                break;
            }

            String type;
            if (spawnedInCurrentTurn == 0) {
                type = "FLAG";
                hugeWaveFlag = true;
            } else {
                List<String> pool = spec.getSpawnPool();
                type = pool.get(random.nextInt(pool.size()));
                hugeWaveFlag = false;
            }

            int lane = random.nextInt(GameBoard.ROWS);
            Zombie zombie = factory.create(type, lane, board);
            spawned.add(zombie);

            spawnedInCurrentTurn++;
            if (spawnedInCurrentTurn >= spec.getZombiesPerTurn()) {
                currentTurnIndex++;
                spawnedInCurrentTurn = 0;
            }

            timeUntilNextSpawn += computeInterval(totalSpawnCount);
            totalSpawnCount++;
        }

        return spawned;
    }

    @Override
    public int getCurrentWaveIndex() {
        return currentTurnIndex;
    }

    @Override
    public int getTotalWaves() {
        return maxTurns != null ? maxTurns : 0;
    }

    @Override
    public boolean isHugeWaveApproaching() {
        return hugeWaveFlag;
    }

    @Override
    public boolean isAllWavesCleared(List<Zombie> activeZombies) {
        if (maxTurns == null) {
            return false;
        }
        return currentTurnIndex >= maxTurns && (activeZombies == null || activeZombies.isEmpty());
    }

    @Override
    public double getProgressRatio(List<Zombie> activeZombies) {
        if (maxTurns == null) {
            return 0.0;
        }
        if (isAllWavesCleared(activeZombies)) {
            return 1.0;
        }
        return Math.min(1.0, Math.max(0.0, (double) currentTurnIndex / maxTurns));
    }

    @Override
    public double getWaveElapsedTime() {
        return totalElapsedTime;
    }

    public SpawnSpec getSpec() {
        return spec;
    }

    public Integer getMaxTurns() {
        return maxTurns;
    }

    public int getTotalSpawnCount() {
        return totalSpawnCount;
    }

    public double getGraceRemaining() {
        return graceRemaining;
    }
}
