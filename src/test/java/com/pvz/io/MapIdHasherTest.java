package com.pvz.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.pvz.builder.CustomMapBuilder;
import com.pvz.builder.GameMap;
import com.pvz.builder.MapBuilder;
import com.pvz.model.tile.TileType;

/**
 * Unit tests for MapIdHasher: which changes give a map a new record table.
 */
class MapIdHasherTest {
    private static MapBuilder base() {
        return new CustomMapBuilder(Set.of("PEASHOOTER", "SUNFLOWER", "LILY_PAD"))
                .setTile(2, 2, TileType.WATER)
                .setAllowedPlants(List.of("PEASHOOTER", "SUNFLOWER"));
    }

    @Test
    @DisplayName("The ID is 64 lowercase hex characters and stable")
    void testFormat() {
        String id = MapIdHasher.hash(base().build());
        assertTrue(id.matches("[0-9a-f]{64}"), id);
        assertEquals(id, MapIdHasher.hash(base().build()));
    }

    @Test
    @DisplayName("Name, sun and turn count do not change the ID")
    void testIgnoredFields() {
        GameMap a = base().build();
        GameMap b = base().setName("Other").setStartingSun(300).setMaxTurns(7).build();
        assertEquals(MapIdHasher.hash(a), MapIdHasher.hash(b));
    }

    @Test
    @DisplayName("Seed bar order does not change the ID")
    void testPlantOrder() {
        GameMap a = base().build();
        GameMap b = base().setAllowedPlants(List.of("SUNFLOWER", "PEASHOOTER")).build();
        assertEquals(MapIdHasher.hash(a), MapIdHasher.hash(b));
    }

    @Test
    @DisplayName("One different tile or plant gives a different ID")
    void testLayoutChanges() {
        String id = MapIdHasher.hash(base().build());
        assertNotEquals(id, MapIdHasher.hash(base().setTile(4, 8, TileType.CRATER).build()));
        assertNotEquals(id, MapIdHasher.hash(
                base().setAllowedPlants(List.of("PEASHOOTER", "SUNFLOWER", "LILY_PAD")).build()));
    }
}
