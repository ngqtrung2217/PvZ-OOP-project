package com.pvz.plant;

import com.pvz.core.GameManager;
import com.pvz.model.economy.CardSlot;
import com.pvz.model.economy.DeckManager;
import com.pvz.model.plant.PlantFactory;
import com.pvz.model.plant.PlantType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeckManagerTest {
    private PlantFactory factory;

    @BeforeEach
    void setUp() {
        factory = new PlantFactory();
        GameManager.getInstance().startNewGame(null);
    }

    @Test
    void filtersAndDeduplicatesAllowedPlantsInOrder() {
        List<PlantType> allowed = Arrays.asList(
                PlantType.PEASHOOTER,
                PlantType.SUNFLOWER,
                PlantType.PEASHOOTER,
                null,
                PlantType.LILY_PAD
        );

        DeckManager deck = new DeckManager(allowed, factory);

        assertEquals(
                List.of(PlantType.PEASHOOTER, PlantType.SUNFLOWER, PlantType.LILY_PAD),
                deck.getAllowedPlantTypes()
        );
        assertTrue(deck.hasPlant(PlantType.PEASHOOTER));
        assertTrue(deck.hasPlant(PlantType.LILY_PAD));
        assertFalse(deck.hasPlant(PlantType.WALL_NUT));
        assertFalse(deck.hasPlant(PlantType.CHERRY_BOMB));
    }

    @Test
    void preventsSelectingUnaffordableOrDisallowedPlant() {
        DeckManager deck = new DeckManager(
                List.of(PlantType.SUNFLOWER, PlantType.PEASHOOTER),
                factory
        );

        // Starting sun is 50: Sunflower (50) is selectable, Peashooter (100) is not
        assertFalse(deck.selectPlant(PlantType.PEASHOOTER, 50));
        assertNull(deck.getSelectedPlantType());

        // WallNut is not in allowedPlants
        assertFalse(deck.selectPlant(PlantType.WALL_NUT, 500));

        assertTrue(deck.selectPlant(PlantType.SUNFLOWER, 50));
        assertEquals(PlantType.SUNFLOWER, deck.getSelectedPlantType());
    }

    @Test
    void consumesSunAndTriggersCooldownWhenPlanting() {
        GameManager gm = GameManager.getInstance();
        gm.addSun(100); // Total 150 sun

        DeckManager deck = new DeckManager(List.of(PlantType.PEASHOOTER), factory);
        CardSlot peaSlot = deck.getSlot(PlantType.PEASHOOTER);
        assertNotNull(peaSlot);

        // Click inside the card slot
        double clickX = peaSlot.getBounds().getMinX() + 10;
        double clickY = peaSlot.getBounds().getMinY() + 10;
        assertTrue(deck.handleMouseClick(clickX, clickY, gm.getSun()));
        assertEquals(PlantType.PEASHOOTER, deck.getSelectedPlantType());

        PlantType planted = deck.consumeSelectedPlant(gm);
        assertEquals(PlantType.PEASHOOTER, planted);
        assertEquals(50, gm.getSun());
        assertNull(deck.getSelectedPlantType());
        assertFalse(peaSlot.isReady());

        // Cooldown only advances when update(deltaTime) is called (pause-safe)
        double cooldown = factory.getSpec(PlantType.PEASHOOTER).getCooldown();
        deck.update(cooldown / 2.0);
        assertFalse(peaSlot.isReady());
        assertEquals(0.5, peaSlot.getCooldownTimer().getProgress(), 1e-6);

        deck.update(cooldown / 2.0);
        assertTrue(peaSlot.isReady());
        assertEquals(0.0, peaSlot.getCooldownTimer().getProgress(), 1e-6);
    }
}
