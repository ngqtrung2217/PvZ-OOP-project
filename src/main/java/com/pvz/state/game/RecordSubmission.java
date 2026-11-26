package com.pvz.state.game;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import com.pvz.builder.GameMap;
import com.pvz.io.MapIdHasher;
import com.pvz.io.RecordRepository;
import com.pvz.io.RunRecord;

/**
 * What the end screen shows about records: the rank the game earned and the layout's
 * top table.
 *
 * @param rank  1 for the best, or -1 if not ranked (also for a play-test)
 * @param top   the layout's best records after this game, best first
 * @param error a message if the record could not be saved, otherwise null
 */
public record RecordSubmission(int rank, List<RunRecord> top, String error) {

    public RecordSubmission {
        top = List.copyOf(top);
    }

    public boolean isNewRecord() {
        return rank > 0;
    }

    /**
     * Saves a finished game to the record table of its layout, then reads the table back.
     * A play-test is never saved (README 3.8) but still shows the table.
     */
    public static RecordSubmission submit(RunResult result, RecordRepository records) {
        GameMap map = result.map();
        String layoutId = MapIdHasher.hash(map);
        if (result.practice()) {
            return new RecordSubmission(-1, records.top(layoutId, RecordRepository.MAX_PER_LAYOUT), null);
        }

        RunRecord record = new RunRecord(map.getStartingSun(), map.getMaxTurns(),
                result.wavesCleared(), result.survivalSeconds(),
                result.outcome() == RunResult.Outcome.WON, result.kills(), result.drowned(),
                result.score(), Instant.now());
        int rank = -1;
        String error = null;
        try {
            rank = records.submit(layoutId, record);
        } catch (IOException e) {
            error = "Không lưu được kỷ lục: " + e.getMessage();
        }
        return new RecordSubmission(rank, records.top(layoutId, RecordRepository.MAX_PER_LAYOUT), error);
    }
}
