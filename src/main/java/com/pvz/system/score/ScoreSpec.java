package com.pvz.system.score;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

/**
 * Every scoring constant from README 3.7, read from {@code resources/data/score_spec.json}
 * so the team can balance scoring without touching code.
 */
public final class ScoreSpec {
    private static final String RESOURCE = "/data/score_spec.json";
    private static volatile ScoreSpec cached;

    // Filled by Gson; the names match the JSON keys
    private Map<String, Integer> killPoints;
    private double drownedKillFactor;
    private double pointsPerSecond;
    private int turnBonus;
    private int winBonus;
    private int winBonusPerMower;
    private double sunFactorBase;
    private double sunFactorMin;
    private double sunFactorMax;

    private ScoreSpec() {
    }

    /** The game's spec, read once and then reused. */
    public static ScoreSpec load() {
        if (cached == null) {
            synchronized (ScoreSpec.class) {
                if (cached == null) {
                    cached = read();
                }
            }
        }
        return cached;
    }

    /**
     * @throws IllegalStateException if the file is missing or broken; it ships with the
     *                               game, so that is a bug, not a player mistake
     */
    private static ScoreSpec read() {
        try (InputStream in = ScoreSpec.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Missing " + RESOURCE);
            }
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                ScoreSpec spec = new Gson().fromJson(reader, ScoreSpec.class);
                if (spec == null || spec.killPoints == null) {
                    throw new IllegalStateException(RESOURCE + " has no killPoints");
                }
                spec.killPoints = Map.copyOf(spec.killPoints);
                return spec;
            }
        } catch (IOException | JsonParseException e) {
            throw new IllegalStateException("Broken " + RESOURCE, e);
        }
    }

    /** Points for killing one zombie of this type; 0 for a type the spec does not list. */
    public int killPoints(String zombieType) {
        return killPoints.getOrDefault(zombieType, 0);
    }

    public double getDrownedKillFactor() {
        return drownedKillFactor;
    }

    public double getPointsPerSecond() {
        return pointsPerSecond;
    }

    public int getTurnBonus() {
        return turnBonus;
    }

    public int getWinBonus() {
        return winBonus;
    }

    public int getWinBonusPerMower() {
        return winBonusPerMower;
    }

    public double getSunFactorBase() {
        return sunFactorBase;
    }

    public double getSunFactorMin() {
        return sunFactorMin;
    }

    public double getSunFactorMax() {
        return sunFactorMax;
    }
}
