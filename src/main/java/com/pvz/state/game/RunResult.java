package com.pvz.state.game;

import com.pvz.builder.GameMap;

/**
 * What happened in one finished game, handed from {@link PlayingState} to the end screen.
 *
 * @param map             the map that was played, so the end screen can offer a retry
 * @param outcome         whether the player won or lost
 * @param survivalSeconds time spent playing; pauses are not counted
 * @param wavesCleared    waves (campaign) the player got through completely
 * @param practice        an editor play-test, which never keeps a record
 * @param kills           zombies killed by plants or lawn mowers
 * @param drowned         zombies that drowned
 * @param mowersLeft      lawn mowers never used
 * @param score           final score (README 3.7)
 */
public record RunResult(GameMap map, Outcome outcome, double survivalSeconds,
        int wavesCleared, boolean practice, int kills, int drowned, int mowersLeft,
        int score) {

    public enum Outcome {
        WON,
        LOST
    }

    /** A normal game with no kills or score, mainly for tests. */
    public RunResult(GameMap map, Outcome outcome, double survivalSeconds, int wavesCleared) {
        this(map, outcome, survivalSeconds, wavesCleared, false);
    }

    /** A game with no kills or score, mainly for tests. */
    public RunResult(GameMap map, Outcome outcome, double survivalSeconds, int wavesCleared,
            boolean practice) {
        this(map, outcome, survivalSeconds, wavesCleared, practice, 0, 0, 0, 0);
    }

    public RunResult {
        if (map == null || outcome == null) {
            throw new IllegalArgumentException("Map and outcome are required");
        }
        if (survivalSeconds < 0 || wavesCleared < 0 || kills < 0 || drowned < 0
                || mowersLeft < 0 || score < 0) {
            throw new IllegalArgumentException("Counts and time cannot be negative");
        }
    }
}
