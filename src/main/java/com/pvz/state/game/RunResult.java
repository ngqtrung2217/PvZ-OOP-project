package com.pvz.state.game;

import com.pvz.builder.GameMap;

/**
 * What happened in one finished game, handed from {@link PlayingState} to the end screen.
 * <p>
 * TODO(Tuan Minh): add kills, drownings, lawn mowers left and score once RunStats exists,
 * so the end screen can submit a record.
 *
 * @param map             the map that was played, so the end screen can offer a retry
 * @param outcome         whether the player won or lost
 * @param survivalSeconds time spent playing; pauses are not counted
 * @param wavesCleared    waves (campaign) the player got through completely
 * @param practice        an editor play-test, which never keeps a record
 */
public record RunResult(GameMap map, Outcome outcome, double survivalSeconds,
        int wavesCleared, boolean practice) {

    public enum Outcome {
        WON,
        LOST
    }

    /** A normal (not practice) game. */
    public RunResult(GameMap map, Outcome outcome, double survivalSeconds, int wavesCleared) {
        this(map, outcome, survivalSeconds, wavesCleared, false);
    }

    public RunResult {
        if (map == null || outcome == null) {
            throw new IllegalArgumentException("Map and outcome are required");
        }
        if (survivalSeconds < 0 || wavesCleared < 0) {
            throw new IllegalArgumentException("Time and waves cannot be negative");
        }
    }
}
