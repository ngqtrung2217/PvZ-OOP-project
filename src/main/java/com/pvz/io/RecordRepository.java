package com.pvz.io;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.pvz.io.dto.RunRecordDTO;

/**
 * Keeps the top records of each map layout in one JSON file (README 3.8):
 * <pre>
 * { "&lt;layout ID from MapIdHasher&gt;": [ record, record, ... ], ... }
 * </pre>
 * Each layout keeps its {@link #MAX_PER_LAYOUT} best games, ordered by
 * {@link RunRecord#RANKING}. The file is read on every call, so several screens can use
 * their own repository on the same file without going stale.
 */
public final class RecordRepository {
    public static final int MAX_PER_LAYOUT = 10;
    private static final Type FILE_TYPE = new TypeToken<Map<String, List<RunRecordDTO>>>() { }.getType();

    private final Path file;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public RecordRepository(Path file) {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null");
        }
        this.file = file;
    }

    /** The real records file, {@code ~/.pvz/records.json}. */
    public static RecordRepository inUserHome() {
        return new RecordRepository(UserDataPaths.inUserHome().getRecordsFile());
    }

    /**
     * Adds a game to a layout's table and saves the file.
     *
     * @return its rank, 1 for the best, or -1 if it did not make the top
     *         {@value #MAX_PER_LAYOUT}
     * @throws IOException if the file cannot be written; a broken file is moved aside to
     *                     {@code records.json.bak} and a new one started instead
     */
    public int submit(String layoutId, RunRecord record) throws IOException {
        if (layoutId == null || layoutId.isBlank() || record == null) {
            throw new IllegalArgumentException("Layout ID and record are required");
        }
        Map<String, List<RunRecord>> all = loadForWrite();
        List<RunRecord> table = new ArrayList<>(all.getOrDefault(layoutId, List.of()));
        table.add(record);
        table.sort(RunRecord.RANKING);

        // Equal records keep the one played first above, so the new one is found by identity
        int index = -1;
        for (int i = 0; i < table.size(); i++) {
            if (table.get(i) == record) {
                index = i;
                break;
            }
        }
        if (table.size() > MAX_PER_LAYOUT) {
            table = new ArrayList<>(table.subList(0, MAX_PER_LAYOUT));
        }
        all.put(layoutId, table);
        save(all);
        return index < MAX_PER_LAYOUT ? index + 1 : -1;
    }

    /**
     * The best records of a layout, best first. An unreadable file counts as empty here,
     * so a screen can still be shown.
     */
    public List<RunRecord> top(String layoutId, int limit) {
        try {
            List<RunRecord> table = load().getOrDefault(layoutId, List.of());
            return List.copyOf(table.subList(0, Math.min(Math.max(limit, 0), table.size())));
        } catch (IOException | RecordFormatException e) {
            return List.of();
        }
    }

    private Map<String, List<RunRecord>> loadForWrite() throws IOException {
        try {
            return load();
        } catch (RecordFormatException e) {
            // Keep the broken file for inspection instead of silently losing it
            Files.move(file, file.resolveSibling(file.getFileName() + ".bak"),
                    StandardCopyOption.REPLACE_EXISTING);
            return new LinkedHashMap<>();
        }
    }

    private Map<String, List<RunRecord>> load() throws IOException, RecordFormatException {
        if (!Files.exists(file)) {
            return new LinkedHashMap<>();
        }
        Map<String, List<RunRecordDTO>> raw;
        try {
            raw = gson.fromJson(Files.readString(file, StandardCharsets.UTF_8), FILE_TYPE);
        } catch (JsonParseException e) {
            throw new RecordFormatException();
        }
        Map<String, List<RunRecord>> all = new LinkedHashMap<>();
        if (raw == null) {
            return all;
        }
        for (Map.Entry<String, List<RunRecordDTO>> entry : raw.entrySet()) {
            List<RunRecord> table = new ArrayList<>();
            if (entry.getValue() != null) {
                for (RunRecordDTO dto : entry.getValue()) {
                    table.add(fromDto(dto));
                }
            }
            table.sort(RunRecord.RANKING);
            all.put(entry.getKey(), table);
        }
        return all;
    }

    /** Writes to a temporary file first, so a crash mid-write cannot corrupt the records. */
    private void save(Map<String, List<RunRecord>> all) throws IOException {
        Map<String, List<RunRecordDTO>> raw = new LinkedHashMap<>();
        for (Map.Entry<String, List<RunRecord>> entry : all.entrySet()) {
            List<RunRecordDTO> table = new ArrayList<>();
            for (RunRecord record : entry.getValue()) {
                table.add(toDto(record));
            }
            raw.put(entry.getKey(), table);
        }
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(temp, gson.toJson(raw, FILE_TYPE), StandardCharsets.UTF_8);
        try {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static RunRecordDTO toDto(RunRecord record) {
        RunRecordDTO dto = new RunRecordDTO();
        dto.startingSun = record.startingSun();
        dto.maxTurns = record.maxTurns();
        dto.turnsReached = record.turnsReached();
        dto.survivalSeconds = record.survivalSeconds();
        dto.outcome = record.won() ? "WON" : "LOST";
        dto.kills = record.kills();
        dto.drowned = record.drowned();
        dto.score = record.score();
        dto.playedAt = record.playedAt().toString();
        return dto;
    }

    private static RunRecord fromDto(RunRecordDTO dto) throws RecordFormatException {
        if (dto == null || dto.startingSun == null || dto.turnsReached == null
                || dto.survivalSeconds == null || dto.outcome == null || dto.score == null
                || dto.playedAt == null) {
            throw new RecordFormatException();
        }
        try {
            return new RunRecord(dto.startingSun, dto.maxTurns, dto.turnsReached,
                    dto.survivalSeconds, "WON".equals(dto.outcome),
                    dto.kills == null ? 0 : dto.kills, dto.drowned == null ? 0 : dto.drowned,
                    dto.score, Instant.parse(dto.playedAt));
        } catch (DateTimeParseException | IllegalArgumentException e) {
            throw new RecordFormatException();
        }
    }

    /** The records file exists but is not valid; handled inside this class only. */
    private static final class RecordFormatException extends Exception {
        RecordFormatException() {
            super(null, null, false, false);
        }
    }
}
