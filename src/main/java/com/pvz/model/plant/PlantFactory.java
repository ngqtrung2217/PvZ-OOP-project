package com.pvz.model.plant;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pvz.model.board.GameBoard;
import com.pvz.model.plant.attack.Peashooter;
import com.pvz.model.plant.attack.SnowPea;
import com.pvz.model.plant.defense.WallNut;
import com.pvz.model.plant.instant.CherryBomb;
import com.pvz.model.plant.producer.Sunflower;
import com.pvz.model.plant.support.LilyPad;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Factory Method implementation that instantiates concrete Plant subclasses
 * using data-driven stats from plants_spec.json.
 */
public class PlantFactory {
    public static final String DEFAULT_SPEC_PATH = "/data/plants_spec.json";

    @FunctionalInterface
    public interface PlantCreator {
        Plant create(int row, int col, double x, double y, double width, double height, PlantSpec spec);
    }

    private final Map<PlantType, PlantSpec> specs;
    private final Map<PlantType, PlantCreator> registry = new EnumMap<>(PlantType.class);

    public PlantFactory() {
        this(loadSpecsFromResource(DEFAULT_SPEC_PATH));
    }

    /**
     * Constructor accepting an in-memory spec map for isolated unit testing.
     */
    public PlantFactory(Map<PlantType, PlantSpec> specs) {
        this.specs = new EnumMap<>(PlantType.class);
        this.specs.putAll(Objects.requireNonNull(specs, "specs must not be null"));
        registerDefaultPlants();
    }

    private void registerDefaultPlants() {
        register(PlantType.SUNFLOWER, Sunflower::new);
        register(PlantType.PEASHOOTER, Peashooter::new);
        register(PlantType.SNOW_PEA, SnowPea::new);
        register(PlantType.WALL_NUT, WallNut::new);
        register(PlantType.CHERRY_BOMB, CherryBomb::new);
        register(PlantType.LILY_PAD, LilyPad::new);
    }

    /**
     * Registers or overrides a creator for a PlantType (Open/Closed Principle).
     */
    public void register(PlantType type, PlantCreator creator) {
        registry.put(Objects.requireNonNull(type, "type must not be null"),
                Objects.requireNonNull(creator, "creator must not be null"));
    }

    public PlantSpec getSpec(PlantType type) {
        PlantSpec spec = specs.get(type);
        if (spec == null) {
            throw new IllegalArgumentException("Missing PlantSpec for type: " + type);
        }
        return spec;
    }

    public Map<PlantType, PlantSpec> getAllSpecs() {
        return Collections.unmodifiableMap(specs);
    }

    /**
     * Creates a plant sized to a standard GameBoard cell.
     */
    public Plant createPlant(PlantType type, int row, int col, double x, double y) {
        return createPlant(type, row, col, x, y, GameBoard.CELL_WIDTH, GameBoard.CELL_HEIGHT);
    }

    public Plant createPlant(PlantType type, int row, int col,
                             double x, double y, double width, double height) {
        PlantCreator creator = registry.get(type);
        if (creator == null) {
            throw new IllegalArgumentException("Unsupported PlantType: " + type);
        }
        PlantSpec spec = getSpec(type);
        return creator.create(row, col, x, y, width, height, spec);
    }

    private static Map<PlantType, PlantSpec> loadSpecsFromResource(String resourcePath) {
        InputStream stream = PlantFactory.class.getResourceAsStream(resourcePath);
        if (stream == null) {
            throw new IllegalStateException("Cannot find plant spec resource: " + resourcePath);
        }
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            Type mapType = new TypeToken<EnumMap<PlantType, PlantSpec>>() {}.getType();
            Map<PlantType, PlantSpec> loaded = new Gson().fromJson(reader, mapType);
            if (loaded == null || loaded.isEmpty()) {
                throw new IllegalStateException("Plant spec file is empty: " + resourcePath);
            }
            return loaded;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load plant specs from " + resourcePath, e);
        }
    }
}
