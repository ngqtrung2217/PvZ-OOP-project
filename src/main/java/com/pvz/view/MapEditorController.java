package com.pvz.view;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.pvz.builder.CustomMapBuilder;
import com.pvz.builder.GameMap;
import com.pvz.builder.LevelDirector;
import com.pvz.builder.MapBuilder;
import com.pvz.builder.MapIssue;
import com.pvz.builder.MapValidator;
import com.pvz.builder.ValidationResult;
import com.pvz.core.GameManager;
import com.pvz.io.LevelDataLoader;
import com.pvz.io.MapIdHasher;
import com.pvz.io.MapSerializer;
import com.pvz.io.RecordRepository;
import com.pvz.io.RunRecord;
import com.pvz.io.UserDataPaths;
import com.pvz.model.board.GameBoard;
import com.pvz.model.tile.TileType;
import com.pvz.system.score.ScoreCalculator;

/**
 * Everything the map editor can do, with no drawing and no JavaFX, so it can be tested on
 * its own. {@link MapEditorView} draws this state and turns clicks and keys into calls
 * here (Model-View-Controller).
 * <p>
 * The controller keeps its own editable copy of the map, because a {@link MapBuilder}
 * cannot be read back. {@link #buildMap()} makes a fresh builder from it each time.
 */
public final class MapEditorController {
    public static final int SUN_STEP = 25;
    public static final int MAX_SUN = 2000;
    public static final int MAX_TURNS = 99;
    public static final int MAX_NAME_LENGTH = 40;
    private static final int DEFAULT_TURNS = 5;

    private final ScoreCalculator scoreCalculator = ScoreCalculator.createDefault();

    private final List<String> plantIds;
    private final MapSerializer serializer;
    private final UserDataPaths paths;
    private final MapValidator validator;
    private final LevelDirector director = new LevelDirector();
    private final RecordRepository records;
    /** Layout the cached records belong to; recomputed when the layout changes. */
    private String recordsLayoutId;
    private List<RunRecord> layoutRecords = List.of();

    private final TileType[][] tiles = new TileType[GameBoard.ROWS][GameBoard.COLS];
    private final Set<String> allowedPlants = new LinkedHashSet<>();
    private String name = "";
    private int startingSun = GameManager.STARTING_SUN;
    private int turns = DEFAULT_TURNS;
    private boolean endless = true;
    private TileType brush = TileType.WATER;
    private String message;

    /**
     * @param plantIds       every plant the editor offers, in seed-bar order
     * @param attackPlantIds plants that count as attackers for the warnings
     */
    public MapEditorController(List<String> plantIds, Set<String> attackPlantIds,
            MapSerializer serializer, UserDataPaths paths) {
        this.plantIds = List.copyOf(plantIds);
        this.validator = new MapValidator(attackPlantIds);
        this.serializer = serializer;
        this.paths = paths;
        this.records = new RecordRepository(paths.getRecordsFile());
        // A new map: all grass, every plant ticked, endless
        applyDayTemplate();
        allowedPlants.addAll(this.plantIds);
    }

    /** An editor for the real game, saving to ~/.pvz/maps. */
    public static MapEditorController createDefault() {
        // TODO(An Nguyen): take plants and attackers from PlantFactory once it exists
        List<String> plants = LevelDirector.PLANTS_USED;
        return new MapEditorController(plants, Set.of("PEASHOOTER", "SNOW_PEA", "CHERRY_BOMB"),
                new MapSerializer(Set.copyOf(plants)), UserDataPaths.inUserHome());
    }

    private MapBuilder newBuilder() {
        return new CustomMapBuilder(Set.copyOf(plantIds));
    }

    // ---- Terrain ----

    public void setBrush(TileType type) {
        if (type == null) {
            throw new IllegalArgumentException("Brush cannot be null");
        }
        brush = type;
    }

    public TileType getBrush() {
        return brush;
    }

    /** Paints the current brush on a cell. Cells outside the grid are ignored. */
    public void paint(int row, int col) {
        if (row >= 0 && row < GameBoard.ROWS && col >= 0 && col < GameBoard.COLS) {
            tiles[row][col] = brush;
        }
    }

    public TileType getTile(int row, int col) {
        return tiles[row][col];
    }

    public int count(TileType type) {
        int n = 0;
        for (TileType[] row : tiles) {
            for (TileType tile : row) {
                if (tile == type) {
                    n++;
                }
            }
        }
        return n;
    }

    /** Terrain template "Ban ngày" (all grass). Only the terrain changes. */
    public void applyDayTemplate() {
        copyTerrain(director.buildDayTemplate(newBuilder()));
    }

    /** Terrain template "Bể bơi" (water on rows 2 and 3). Only the terrain changes. */
    public void applyPoolTemplate() {
        copyTerrain(director.buildPoolTemplate(newBuilder()));
    }

    private void copyTerrain(GameMap template) {
        for (int r = 0; r < GameBoard.ROWS; r++) {
            for (int c = 0; c < GameBoard.COLS; c++) {
                tiles[r][c] = template.getTileType(r, c);
            }
        }
    }

    // ---- Name ----

    /** Sets the name, cut to {@link #MAX_NAME_LENGTH} characters. */
    public void setName(String value) {
        String text = value == null ? "" : value;
        name = text.length() > MAX_NAME_LENGTH ? text.substring(0, MAX_NAME_LENGTH) : text;
    }

    /** Adds typed text to the name; control characters such as Enter are ignored. */
    public void typeName(String typed) {
        if (typed == null) {
            return;
        }
        String printable = typed.replaceAll("\\p{Cntrl}", "");
        setName(name + printable);
    }

    public void deleteLastNameChar() {
        if (!name.isEmpty()) {
            // Step back a whole code point so an emoji is not cut in half
            name = name.substring(0, name.offsetByCodePoints(name.length(), -1));
        }
    }

    public String getName() {
        return name;
    }

    // ---- Sun, plants, turns ----

    /** Moves the starting sun by {@code steps} × {@link #SUN_STEP}, within 0 and MAX_SUN. */
    public void changeSun(int steps) {
        startingSun = Math.max(0, Math.min(MAX_SUN, startingSun + steps * SUN_STEP));
    }

    public int getStartingSun() {
        return startingSun;
    }

    /** Score multiplier for this sun (README 3.7), from score_spec.json. */
    public double getSunMultiplier() {
        return scoreCalculator.sunMultiplier(startingSun);
    }

    public void togglePlant(String plantId) {
        if (!plantIds.contains(plantId)) {
            throw new IllegalArgumentException("Unknown plant ID: " + plantId);
        }
        if (!allowedPlants.remove(plantId)) {
            allowedPlants.add(plantId);
        }
    }

    public boolean isPlantAllowed(String plantId) {
        return allowedPlants.contains(plantId);
    }

    /** Every plant the editor offers, in seed-bar order. */
    public List<String> getPlantIds() {
        return plantIds;
    }

    /** Checked plants, always in seed-bar order whatever order they were ticked in. */
    public List<String> getAllowedPlants() {
        return plantIds.stream().filter(allowedPlants::contains).toList();
    }

    /** Changes the turn count within 1 and MAX_TURNS; also turns "Vô hạn" off. */
    public void changeTurns(int delta) {
        endless = false;
        turns = Math.max(1, Math.min(MAX_TURNS, turns + delta));
    }

    /** Ticks or unticks "Vô hạn". The turn count is kept for when it is unticked. */
    public void toggleEndless() {
        endless = !endless;
    }

    public boolean isEndless() {
        return endless;
    }

    public int getTurns() {
        return turns;
    }

    // ---- Building, checking, saving, opening ----

    /** A map from the current settings, for play-testing or saving. */
    public GameMap buildMap() {
        MapBuilder builder = newBuilder()
                .setName(name)
                .setStartingSun(startingSun)
                .setAllowedPlants(getAllowedPlants())
                .setMaxTurns(endless ? null : turns);
        for (int r = 0; r < GameBoard.ROWS; r++) {
            for (int c = 0; c < GameBoard.COLS; c++) {
                builder.setTile(r, c, tiles[r][c]);
            }
        }
        return builder.build();
    }

    /** Errors and warnings for the current settings, recomputed on every call. */
    public ValidationResult validate() {
        return validator.validate(buildMap());
    }

    /**
     * Saves to ~/.pvz/maps under the map's name, unless an error blocks it. The outcome is
     * left in {@link #getMessage()}.
     *
     * @return the file written, or null if nothing was saved
     */
    public Path save() {
        ValidationResult result = validate();
        if (!result.canSave()) {
            message = "Chưa lưu: " + joinMessages(result.getErrors());
            return null;
        }
        GameMap map = buildMap();
        try {
            Path file = paths.mapFile(map.getName());
            boolean existed = Files.exists(file);
            serializer.save(map, file);
            message = (existed ? "Đã ghi đè: " : "Đã lưu: ") + file.getFileName();
            return file;
        } catch (IllegalArgumentException e) {
            message = "Tên map không dùng làm tên file được";
        } catch (IOException e) {
            message = "Lỗi khi lưu: " + e.getMessage();
        }
        return null;
    }

    private static String joinMessages(List<MapIssue> issues) {
        List<String> texts = new ArrayList<>();
        for (MapIssue issue : issues) {
            texts.add(issue.getMessage());
        }
        return String.join("; ", texts);
    }

    /** Saved custom maps for the "Mở" list. A broken file is noted in the message. */
    public List<LevelDataLoader.SavedMap> listSavedMaps() {
        try {
            LevelDataLoader.CustomMaps maps = new LevelDataLoader(serializer, paths).loadCustomMaps();
            if (!maps.unreadable().isEmpty()) {
                message = maps.unreadable().size() + " file map bị hỏng nên đã bỏ qua";
            }
            return maps.maps();
        } catch (IOException e) {
            message = "Không đọc được thư mục map";
            return List.of();
        }
    }

    /** Replaces every setting with those of a map. Scripted waves are not editable here. */
    public void load(GameMap map) {
        for (int r = 0; r < GameBoard.ROWS; r++) {
            for (int c = 0; c < GameBoard.COLS; c++) {
                tiles[r][c] = map.getTileType(r, c);
            }
        }
        setName(map.getName());
        startingSun = Math.min(MAX_SUN, map.getStartingSun());
        allowedPlants.clear();
        allowedPlants.addAll(map.getAllowedPlants());
        endless = map.getMaxTurns() == null;
        turns = endless ? DEFAULT_TURNS : Math.min(MAX_TURNS, map.getMaxTurns());
    }

    /** Opens a saved map and says so in the message. */
    public void open(GameMap map) {
        load(map);
        message = "Đã mở: " + (map.getName().isBlank() ? "(chưa đặt tên)" : map.getName());
    }

    /** Last result of save or open, shown under the board; null if there is none. */
    /**
     * Best records of the layout being edited (terrain + plants), best first. The file
     * is read again only when the layout changes or after {@link #refreshRecords()}, so
     * this is cheap to call every frame.
     */
    public List<RunRecord> getLayoutRecords(int limit) {
        String layoutId = MapIdHasher.hash(buildMap());
        if (!layoutId.equals(recordsLayoutId)) {
            recordsLayoutId = layoutId;
            layoutRecords = records.top(layoutId, RecordRepository.MAX_PER_LAYOUT);
        }
        return layoutRecords.subList(0, Math.min(Math.max(limit, 0), layoutRecords.size()));
    }

    /** Forgets cached records, for example after a game may have added one. */
    public void refreshRecords() {
        recordsLayoutId = null;
    }

    public String getMessage() {
        return message;
    }
}
