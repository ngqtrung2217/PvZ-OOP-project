package com.pvz.io;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.pvz.builder.CustomMapBuilder;
import com.pvz.builder.GameMap;
import com.pvz.builder.MapBuilder;
import com.pvz.builder.MapMode;
import com.pvz.builder.SpawnConfig;
import com.pvz.builder.WaveConfig;
import com.pvz.io.dto.MapConfigDTO;
import com.pvz.io.dto.ScriptedWaveDTO;
import com.pvz.model.board.GameBoard;
import com.pvz.model.tile.TileType;

/**
 * Reads and writes map JSON with Gson.
 * <p>
 * Saving: {@code GameMap} → DTO → JSON. Loading: JSON → DTO → {@link MapBuilder} →
 * {@code GameMap}, so a file goes through the same checks as a map made in the editor.
 */
public final class MapSerializer {
    // disableHtmlEscaping keeps characters such as ' readable in map names
    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
    private final Set<String> validPlantIds;

    /**
     * @param validPlantIds every plant ID the game knows; files naming others are rejected.
     */
    public MapSerializer(Set<String> validPlantIds) {
        this.validPlantIds = Set.copyOf(validPlantIds);
    }

    public String toJson(GameMap map) {
        return gson.toJson(toDto(map));
    }

    /** @throws MapFormatException if the text is not JSON or not a valid map */
    public GameMap fromJson(String json) throws MapFormatException {
        MapConfigDTO dto;
        try {
            dto = gson.fromJson(json, MapConfigDTO.class);
        } catch (JsonParseException e) {
            throw new MapFormatException("Not valid map JSON: " + e.getMessage(), e);
        }
        if (dto == null) {
            throw new MapFormatException("Map file is empty");
        }
        try {
            return toMap(dto);
        } catch (IllegalArgumentException | IllegalStateException e) {
            // Thrown by the builder for values it never accepts
            throw new MapFormatException(e.getMessage(), e);
        }
    }

    /** Writes the map as UTF-8, creating parent folders as needed. */
    public void save(GameMap map, Path file) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(file, toJson(map), StandardCharsets.UTF_8);
    }

    public GameMap load(Path file) throws IOException, MapFormatException {
        return fromJson(Files.readString(file, StandardCharsets.UTF_8));
    }

    private static MapConfigDTO toDto(GameMap map) {
        MapConfigDTO dto = new MapConfigDTO();
        dto.name = map.getName();
        dto.mode = map.getMode().name();
        dto.tiles = new String[GameBoard.ROWS][GameBoard.COLS];
        for (int r = 0; r < GameBoard.ROWS; r++) {
            for (int c = 0; c < GameBoard.COLS; c++) {
                dto.tiles[r][c] = map.getTileType(r, c).name();
            }
        }
        dto.startingSun = map.getStartingSun();
        dto.allowedPlants = new ArrayList<>(map.getAllowedPlants());
        dto.maxTurns = map.getMaxTurns();

        if (map.getMode() == MapMode.CAMPAIGN) {
            dto.waves = new ArrayList<>();
            for (WaveConfig wave : map.getWaves()) {
                dto.waves.add(toDto(wave));
            }
        }
        return dto;
    }

    private static ScriptedWaveDTO toDto(WaveConfig wave) {
        ScriptedWaveDTO dto = new ScriptedWaveDTO();
        dto.hugeWave = wave.isHugeWave();
        dto.spawns = new ArrayList<>();
        for (SpawnConfig spawn : wave.getSpawns()) {
            ScriptedWaveDTO.SpawnDTO s = new ScriptedWaveDTO.SpawnDTO();
            s.zombieType = spawn.getZombieType();
            s.lane = spawn.getLane();
            s.spawnDelay = spawn.getSpawnDelay();
            dto.spawns.add(s);
        }
        return dto;
    }

    private GameMap toMap(MapConfigDTO dto) throws MapFormatException {
        MapBuilder builder = new CustomMapBuilder(validPlantIds);
        builder.setName(dto.name == null ? "" : dto.name);

        requireGridShape(dto.tiles);
        for (int r = 0; r < GameBoard.ROWS; r++) {
            for (int c = 0; c < GameBoard.COLS; c++) {
                builder.setTile(r, c, parseTile(dto.tiles[r][c], r, c));
            }
        }

        if (dto.startingSun == null) {
            throw new MapFormatException("startingSun is missing");
        }
        builder.setStartingSun(dto.startingSun);
        builder.setAllowedPlants(dto.allowedPlants == null ? List.of() : dto.allowedPlants);
        builder.setMaxTurns(dto.maxTurns);

        if (dto.waves != null) {
            for (int i = 0; i < dto.waves.size(); i++) {
                builder.addWave(toWave(dto.waves.get(i), i));
            }
        }

        GameMap map = builder.build();
        if (dto.mode != null && !dto.mode.equals(map.getMode().name())) {
            throw new MapFormatException("mode is " + dto.mode + " but the map is "
                    + map.getMode() + " (a campaign map needs waves, a custom map has none)");
        }
        return map;
    }

    private static void requireGridShape(String[][] tiles) throws MapFormatException {
        if (tiles == null) {
            throw new MapFormatException("tiles is missing");
        }
        if (tiles.length != GameBoard.ROWS) {
            throw new MapFormatException("tiles must have " + GameBoard.ROWS + " rows, found "
                    + tiles.length);
        }
        for (int r = 0; r < tiles.length; r++) {
            if (tiles[r] == null || tiles[r].length != GameBoard.COLS) {
                throw new MapFormatException("tiles row " + r + " must have " + GameBoard.COLS
                        + " cells");
            }
        }
    }

    private static TileType parseTile(String name, int row, int col) throws MapFormatException {
        if (name != null) {
            for (TileType type : TileType.values()) {
                if (type.name().equals(name)) {
                    return type;
                }
            }
        }
        throw new MapFormatException("Unknown tile " + name + " at (" + row + ", " + col + ")");
    }

    private static WaveConfig toWave(ScriptedWaveDTO dto, int index) throws MapFormatException {
        if (dto == null || dto.spawns == null) {
            throw new MapFormatException("Wave " + index + " has no spawns");
        }
        List<SpawnConfig> spawns = new ArrayList<>();
        for (ScriptedWaveDTO.SpawnDTO s : dto.spawns) {
            if (s == null || s.lane == null || s.spawnDelay == null) {
                throw new MapFormatException("Wave " + index
                        + " has a spawn without a lane or spawnDelay");
            }
            spawns.add(new SpawnConfig(s.zombieType, s.lane, s.spawnDelay));
        }
        return new WaveConfig(spawns, dto.hugeWave);
    }
}
