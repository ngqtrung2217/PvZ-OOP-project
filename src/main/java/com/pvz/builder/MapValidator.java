package com.pvz.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.pvz.model.board.GameBoard;
import com.pvz.model.tile.TileType;

/**
 * Checks a built map against the editor rules in README section 3.9 and reports every
 * issue at once. Used for both modes, so campaign levels can be checked in tests too.
 * <p>
 * The builder already rejects values that are never valid; this class judges maps that
 * are complete enough to play but may not be ready to save.
 */
public final class MapValidator {
    // TODO(An Nguyen): switch to PlantType constants once model/plant exists
    static final String LILY_PAD = "LILY_PAD";
    static final String SUNFLOWER = "SUNFLOWER";

    private final Set<String> attackPlantIds;

    /**
     * @param attackPlantIds plants that damage zombies. Passed in rather than listed here,
     *                       so adding an attack plant does not mean editing this class.
     */
    public MapValidator(Set<String> attackPlantIds) {
        this.attackPlantIds = Set.copyOf(attackPlantIds);
    }

    public ValidationResult validate(GameMap map) {
        List<MapIssue> issues = new ArrayList<>();
        List<String> plants = map.getAllowedPlants();

        if (map.getName().isBlank()) {
            issues.add(MapIssue.MISSING_NAME);
        }
        if (plants.isEmpty()) {
            issues.add(MapIssue.NO_PLANTS);
        }
        if (hasTile(map, TileType.WATER) && !plants.contains(LILY_PAD)) {
            issues.add(MapIssue.WATER_WITHOUT_LILY_PAD);
        }
        if (!plants.contains(SUNFLOWER)) {
            issues.add(MapIssue.NO_SUNFLOWER);
        }
        if (plants.stream().noneMatch(attackPlantIds::contains)) {
            issues.add(MapIssue.NO_ATTACK_PLANT);
        }
        if (hasRowOfOnly(map, TileType.CRATER)) {
            issues.add(MapIssue.ROW_ALL_CRATER);
        }
        return new ValidationResult(issues);
    }

    private static boolean hasTile(GameMap map, TileType type) {
        for (int r = 0; r < GameBoard.ROWS; r++) {
            for (int c = 0; c < GameBoard.COLS; c++) {
                if (map.getTileType(r, c) == type) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasRowOfOnly(GameMap map, TileType type) {
        for (int r = 0; r < GameBoard.ROWS; r++) {
            boolean allMatch = true;
            for (int c = 0; c < GameBoard.COLS; c++) {
                if (map.getTileType(r, c) != type) {
                    allMatch = false;
                    break;
                }
            }
            if (allMatch) {
                return true;
            }
        }
        return false;
    }
}
