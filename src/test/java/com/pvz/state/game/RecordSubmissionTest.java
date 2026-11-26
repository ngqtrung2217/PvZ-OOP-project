package com.pvz.state.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.pvz.builder.CustomMapBuilder;
import com.pvz.builder.GameMap;
import com.pvz.builder.SpawnConfig;
import com.pvz.builder.WaveConfig;
import com.pvz.core.GameManager;
import com.pvz.io.MapIdHasher;
import com.pvz.io.RecordRepository;
import com.pvz.io.RunRecord;
import com.pvz.model.tile.TileType;

/**
 * Unit tests for saving records from the end screens.
 */
class RecordSubmissionTest {
    @TempDir
    Path dir;
    private RecordRepository repo;
    private GameManager manager;
    private GameMap map;

    @BeforeEach
    void setUp() {
        repo = new RecordRepository(dir.resolve("records.json"));
        manager = GameManager.getInstance();
        manager.setRecordRepository(repo);
        map = new CustomMapBuilder(Set.of()).setStartingSun(100).setMaxTurns(4).build();
        manager.startNewGame(map);
    }

    private static RunResult result(GameMap map, int score, boolean practice) {
        return new RunResult(map, RunResult.Outcome.LOST, 75.0, 2, practice, 6, 1, 3, score);
    }

    @Test
    @DisplayName("Entering the game-over screen saves the game once, with every field")
    void testGameOverSaves() {
        GameOverState over = new GameOverState(result(map, 400, false));
        manager.changeState(over);
        over.enter(); // a second enter must not save twice

        RecordSubmission records = over.getRecords();
        assertEquals(1, records.rank());
        assertTrue(records.isNewRecord());
        assertNull(records.error());

        assertEquals(1, repo.top(MapIdHasher.hash(map), 10).size());
        RunRecord saved = records.top().get(0);
        assertEquals(100, saved.startingSun());
        assertEquals(4, saved.maxTurns());
        assertEquals(2, saved.turnsReached());
        assertEquals(75.0, saved.survivalSeconds(), 1e-9);
        assertFalse(saved.won());
        assertEquals(6, saved.kills());
        assertEquals(1, saved.drowned());
        assertEquals(400, saved.score());
    }

    @Test
    @DisplayName("A better game on the same layout takes rank 1, even with other sun and turns")
    void testSameLayoutSharesTable() {
        manager.changeState(new GameOverState(result(map, 400, false)));

        GameMap sameLayout = new CustomMapBuilder(Set.of()).setStartingSun(300).build();
        VictoryState win = new VictoryState(new RunResult(sameLayout, RunResult.Outcome.WON,
                90, 4, false, 10, 0, 5, 900));
        manager.changeState(win);

        assertEquals(1, win.getRecords().rank());
        assertEquals(2, win.getRecords().top().size());
        assertEquals(400, win.getRecords().top().get(1).score());
    }

    @Test
    @DisplayName("A different layout gets its own table")
    void testOtherLayout() {
        manager.changeState(new GameOverState(result(map, 400, false)));
        GameMap other = new CustomMapBuilder(Set.of()).setTile(0, 0, TileType.CRATER).build();

        GameOverState over = new GameOverState(result(other, 100, false));
        manager.changeState(over);

        assertEquals(1, over.getRecords().rank());
        assertEquals(1, over.getRecords().top().size());
    }

    @Test
    @DisplayName("A play-test is never saved but still shows the table")
    void testPracticeNotSaved() {
        manager.changeState(new GameOverState(result(map, 400, false)));

        GameOverState practice = new GameOverState(result(map, 9999, true), new MenuState());
        manager.changeState(practice);

        assertEquals(-1, practice.getRecords().rank());
        assertEquals(1, practice.getRecords().top().size());
        assertEquals(400, practice.getRecords().top().get(0).score());
    }

    @Test
    @DisplayName("A real game that ends saves through the whole flow")
    void testFullFlow() {
        // One zombie, which the lawn mower stops, so the level is won
        GameMap level = new CustomMapBuilder(Set.of())
                .addWave(new WaveConfig(List.of(new SpawnConfig("NORMAL", 0, 0.0)), false))
                .build();
        manager.startNewGame(level);
        PlayingState playing = (PlayingState) manager.getCurrentState();
        for (int i = 0; i < 2000 && manager.getCurrentState() == playing; i++) {
            playing.update(0.1);
        }

        VictoryState win = assertInstanceOf(VictoryState.class, manager.getCurrentState());
        assertTrue(win.getResult().score() > 0);
        assertEquals(win.getResult().score(), win.getRecords().top().get(0).score());
        assertEquals(1, repo.top(MapIdHasher.hash(level), 10).size());
    }
}
