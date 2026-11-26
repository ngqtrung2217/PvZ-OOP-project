package com.pvz.io;

import java.time.Instant;
import java.util.Comparator;

/**
 * One finished game in a layout's record table (README 3.8).
 *
 * @param startingSun     the map's starting sun
 * @param maxTurns        the map's turn limit, or null for endless or campaign
 * @param turnsReached    turns or waves fully cleared
 * @param survivalSeconds time played, pauses not counted
 * @param won             whether the game was won
 * @param kills           zombies killed
 * @param drowned         zombies drowned
 * @param score           final score
 * @param playedAt        when the game ended
 */
public record RunRecord(int startingSun, Integer maxTurns, int turnsReached,
        double survivalSeconds, boolean won, int kills, int drowned, int score,
        Instant playedAt) {

    /**
     * Best first: higher score; on equal score, longer survival; still equal, the game
     * played first.
     */
    public static final Comparator<RunRecord> RANKING = Comparator
            .comparingInt(RunRecord::score).reversed()
            .thenComparing(Comparator.comparingDouble(RunRecord::survivalSeconds).reversed())
            .thenComparing(RunRecord::playedAt);

    public RunRecord {
        if (playedAt == null) {
            throw new IllegalArgumentException("playedAt is required");
        }
        if (startingSun < 0 || turnsReached < 0 || survivalSeconds < 0 || kills < 0
                || drowned < 0 || score < 0) {
            throw new IllegalArgumentException("Counts and time cannot be negative");
        }
    }
}
