package com.pvz.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for RecordRepository: ranking, the top-10 limit and the records file.
 */
class RecordRepositoryTest {
    private static final String LAYOUT = "a".repeat(64);
    private static final String OTHER_LAYOUT = "b".repeat(64);
    private static final Instant T0 = Instant.parse("2026-11-27T10:00:00Z");

    @TempDir
    Path dir;
    private Path file;
    private RecordRepository repo;

    @BeforeEach
    void setUp() {
        file = dir.resolve("records.json");
        repo = new RecordRepository(file);
    }

    /** A record with the given score and time, played {@code minute} minutes after T0. */
    private static RunRecord record(int score, double seconds, int minute) {
        return new RunRecord(150, 5, 2, seconds, false, 3, 0, score, T0.plusSeconds(60L * minute));
    }

    @Test
    @DisplayName("A missing file means no records")
    void testEmpty() {
        assertTrue(repo.top(LAYOUT, 10).isEmpty());
        assertTrue(!Files.exists(file));
    }

    @Test
    @DisplayName("Submit returns the rank; higher scores go above")
    void testRanking() throws Exception {
        assertEquals(1, repo.submit(LAYOUT, record(100, 60, 0)));
        assertEquals(1, repo.submit(LAYOUT, record(300, 60, 1)));
        assertEquals(2, repo.submit(LAYOUT, record(200, 60, 2)));

        List<RunRecord> top = repo.top(LAYOUT, 10);
        assertEquals(List.of(300, 200, 100), top.stream().map(RunRecord::score).toList());
    }

    @Test
    @DisplayName("Equal score: longer survival wins; still equal, the earlier game stays above")
    void testTieBreaks() throws Exception {
        repo.submit(LAYOUT, record(100, 60, 0));
        assertEquals(1, repo.submit(LAYOUT, record(100, 90, 1)), "Longer survival goes above");
        assertEquals(3, repo.submit(LAYOUT, record(100, 60, 2)), "Same as an earlier game: below it");

        List<RunRecord> top = repo.top(LAYOUT, 10);
        assertEquals(90, top.get(0).survivalSeconds(), 1e-9);
        assertEquals(T0, top.get(1).playedAt());
        assertEquals(T0.plusSeconds(120), top.get(2).playedAt());
    }

    @Test
    @DisplayName("Only the best 10 are kept; a game below them gets -1")
    void testTopTenLimit() throws Exception {
        for (int i = 1; i <= RecordRepository.MAX_PER_LAYOUT; i++) {
            repo.submit(LAYOUT, record(i * 10, 60, i));
        }
        assertEquals(-1, repo.submit(LAYOUT, record(5, 60, 20)));
        assertEquals(1, repo.submit(LAYOUT, record(1000, 60, 21)));

        List<RunRecord> top = repo.top(LAYOUT, 99);
        assertEquals(RecordRepository.MAX_PER_LAYOUT, top.size());
        assertEquals(1000, top.get(0).score());
        assertEquals(20, top.get(top.size() - 1).score(), "The old 10th place dropped out");
    }

    @Test
    @DisplayName("Each layout has its own table")
    void testLayoutsAreSeparate() throws Exception {
        repo.submit(LAYOUT, record(100, 60, 0));
        assertEquals(1, repo.submit(OTHER_LAYOUT, record(50, 60, 1)));

        assertEquals(1, repo.top(LAYOUT, 10).size());
        assertEquals(50, repo.top(OTHER_LAYOUT, 10).get(0).score());
    }

    @Test
    @DisplayName("Records survive a new repository on the same file, every field intact")
    void testPersistence() throws Exception {
        RunRecord endless = new RunRecord(50, null, 3, 125.5, true, 12, 2, 840, T0);
        repo.submit(LAYOUT, endless);

        RunRecord loaded = new RecordRepository(file).top(LAYOUT, 1).get(0);

        assertEquals(endless, loaded);
        assertNull(loaded.maxTurns());
        assertTrue(Files.readString(file).contains("\"outcome\": \"WON\""));
    }

    @Test
    @DisplayName("top() respects the limit")
    void testTopLimit() throws Exception {
        for (int i = 0; i < 5; i++) {
            repo.submit(LAYOUT, record(i, 60, i));
        }
        assertEquals(3, repo.top(LAYOUT, 3).size());
        assertEquals(0, repo.top(LAYOUT, -1).size());
    }

    @Test
    @DisplayName("A broken file reads as empty and is kept as .bak when a new record is saved")
    void testBrokenFile() throws Exception {
        Files.writeString(file, "{ not json");

        assertTrue(repo.top(LAYOUT, 10).isEmpty());
        assertEquals(1, repo.submit(LAYOUT, record(100, 60, 0)));

        assertEquals("{ not json", Files.readString(dir.resolve("records.json.bak")));
        assertEquals(1, repo.top(LAYOUT, 10).size());
    }

    @Test
    @DisplayName("Bad arguments are rejected")
    void testBadArguments() {
        assertThrows(IllegalArgumentException.class, () -> repo.submit(" ", record(1, 1, 0)));
        assertThrows(IllegalArgumentException.class, () -> repo.submit(LAYOUT, null));
        assertThrows(IllegalArgumentException.class,
                () -> new RunRecord(50, null, 0, -1, false, 0, 0, 0, T0));
    }
}
