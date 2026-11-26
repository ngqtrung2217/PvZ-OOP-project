package com.pvz.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.pvz.model.board.GameBoard;
import com.pvz.model.tile.TileType;

/**
 * Unit tests for MapValidator: the save rules of README section 3.9, in both map modes.
 */
class MapValidatorTest {
    private static final Set<String> ALL_PLANTS =
            Set.of("PEASHOOTER", "SNOW_PEA", "SUNFLOWER", "WALL_NUT", "LILY_PAD");
    private static final List<String> GOOD_PLANTS =
            List.of("PEASHOOTER", "SUNFLOWER", "WALL_NUT", "LILY_PAD");

    private MapValidator validator;
    private MapBuilder builder;

    @BeforeEach
    void setUp() {
        validator = new MapValidator(Set.of("PEASHOOTER", "SNOW_PEA"));
        // A map that passes every rule; each test breaks one of them
        builder = new CustomMapBuilder(ALL_PLANTS)
                .setName("Ao giua san")
                .setAllowedPlants(GOOD_PLANTS)
                .setTile(2, 4, TileType.WATER);
    }

    @Test
    @DisplayName("A complete custom map has no errors or warnings")
    void testValidCustomMap() {
        ValidationResult result = validator.validate(builder.setMaxTurns(5).build());
        assertTrue(result.canSave());
        assertTrue(result.getErrors().isEmpty());
        assertTrue(result.getWarnings().isEmpty());
    }

    @Test
    @DisplayName("An endless custom map is valid too")
    void testValidEndlessMap() {
        ValidationResult result = validator.validate(builder.setMaxTurns(null).build());
        assertTrue(result.canSave());
        assertTrue(result.getWarnings().isEmpty());
    }

    @Test
    @DisplayName("A complete campaign map has no errors or warnings")
    void testValidCampaignMap() {
        GameMap map = builder.addWave(new WaveConfig(
                List.of(new SpawnConfig("NORMAL", 0, 1.0)), false)).build();

        assertEquals(MapMode.CAMPAIGN, map.getMode());
        ValidationResult result = validator.validate(map);
        assertTrue(result.canSave());
        assertTrue(result.getWarnings().isEmpty());
    }

    @Test
    @DisplayName("A blank name blocks saving")
    void testMissingName() {
        ValidationResult result = validator.validate(builder.setName("  ").build());
        assertFalse(result.canSave());
        assertEquals(List.of(MapIssue.MISSING_NAME), result.getErrors());
    }

    @Test
    @DisplayName("No plants blocks saving")
    void testNoPlants() {
        ValidationResult result = validator.validate(builder.setAllowedPlants(List.of()).build());
        assertFalse(result.canSave());
        assertTrue(result.getErrors().contains(MapIssue.NO_PLANTS));
    }

    @Test
    @DisplayName("Every issue is reported at once, errors and warnings separated")
    void testAllIssuesAtOnce() {
        GameMap map = builder.setName("").setAllowedPlants(List.of()).build();
        ValidationResult result = validator.validate(map);

        assertEquals(List.of(MapIssue.MISSING_NAME, MapIssue.NO_PLANTS), result.getErrors());
        assertEquals(List.of(MapIssue.WATER_WITHOUT_LILY_PAD, MapIssue.NO_SUNFLOWER,
                MapIssue.NO_ATTACK_PLANT), result.getWarnings());
    }

    @Test
    @DisplayName("Water without LilyPad warns but still saves")
    void testWaterWithoutLilyPad() {
        ValidationResult result = validator.validate(
                builder.setAllowedPlants(List.of("PEASHOOTER", "SUNFLOWER")).build());
        assertTrue(result.canSave());
        assertEquals(List.of(MapIssue.WATER_WITHOUT_LILY_PAD), result.getWarnings());
    }

    @Test
    @DisplayName("No water means LilyPad is not needed")
    void testNoWaterNoLilyPadNeeded() {
        GameMap map = builder.setTile(2, 4, TileType.GRASS)
                .setAllowedPlants(List.of("PEASHOOTER", "SUNFLOWER"))
                .build();
        assertFalse(validator.validate(map).has(MapIssue.WATER_WITHOUT_LILY_PAD));
    }

    @Test
    @DisplayName("No Sunflower warns")
    void testNoSunflower() {
        ValidationResult result = validator.validate(
                builder.setAllowedPlants(List.of("PEASHOOTER", "LILY_PAD")).build());
        assertTrue(result.canSave());
        assertEquals(List.of(MapIssue.NO_SUNFLOWER), result.getWarnings());
    }

    @Test
    @DisplayName("No attack plant warns; any one attack plant is enough")
    void testAttackPlant() {
        ValidationResult none = validator.validate(
                builder.setAllowedPlants(List.of("SUNFLOWER", "WALL_NUT", "LILY_PAD")).build());
        assertEquals(List.of(MapIssue.NO_ATTACK_PLANT), none.getWarnings());

        ValidationResult snowPea = validator.validate(
                builder.setAllowedPlants(List.of("SNOW_PEA", "SUNFLOWER", "LILY_PAD")).build());
        assertFalse(snowPea.has(MapIssue.NO_ATTACK_PLANT));
    }

    @Test
    @DisplayName("A full row of craters warns; a partial one does not")
    void testRowAllCrater() {
        for (int c = 0; c < GameBoard.COLS - 1; c++) {
            builder.setTile(4, c, TileType.CRATER);
        }
        assertFalse(validator.validate(builder.build()).has(MapIssue.ROW_ALL_CRATER));

        builder.setTile(4, GameBoard.COLS - 1, TileType.CRATER);
        ValidationResult result = validator.validate(builder.build());
        assertTrue(result.canSave());
        assertEquals(List.of(MapIssue.ROW_ALL_CRATER), result.getWarnings());
    }

    @Test
    @DisplayName("Result lists cannot be modified")
    void testResultIsReadOnly() {
        ValidationResult result = validator.validate(builder.setName("").build());
        assertThrows(UnsupportedOperationException.class,
                () -> result.getErrors().add(MapIssue.NO_PLANTS));
        assertThrows(UnsupportedOperationException.class,
                () -> result.getWarnings().clear());
    }
}
