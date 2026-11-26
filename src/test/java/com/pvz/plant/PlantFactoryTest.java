package com.pvz.plant;

import com.pvz.model.board.GameBoard;
import com.pvz.model.plant.Plant;
import com.pvz.model.plant.PlantFactory;
import com.pvz.model.plant.PlantLayer;
import com.pvz.model.plant.PlantSpec;
import com.pvz.model.plant.PlantType;
import com.pvz.model.plant.attack.Peashooter;
import com.pvz.model.plant.attack.SnowPea;
import com.pvz.model.plant.defense.WallNut;
import com.pvz.model.plant.instant.CherryBomb;
import com.pvz.model.plant.producer.Sunflower;
import com.pvz.model.plant.support.LilyPad;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlantFactoryTest {
    private PlantFactory factory;

    @BeforeEach
    void setUp() {
        factory = new PlantFactory();
    }

    @Test
    void loadsSpecsForAllPlantTypesFromJson() {
        for (PlantType type : PlantType.values()) {
            PlantSpec spec = factory.getSpec(type);
            assertNotNull(spec, "Missing spec for " + type);
            assertTrue(spec.getHp() > 0, "HP must be positive for " + type);
            assertTrue(spec.getCost() >= 0, "Cost must be non-negative for " + type);
            assertNotNull(spec.getLayer(), "Layer must not be null for " + type);
        }
    }

    @Test
    void createsCorrectConcreteSubclassAndLayerForEachPlantType() {
        double x = GameBoard.START_X;
        double y = GameBoard.START_Y;

        Plant sunflower = factory.createPlant(PlantType.SUNFLOWER, 0, 0, x, y);
        Plant peashooter = factory.createPlant(PlantType.PEASHOOTER, 0, 1, x, y);
        Plant snowPea = factory.createPlant(PlantType.SNOW_PEA, 1, 0, x, y);
        Plant wallNut = factory.createPlant(PlantType.WALL_NUT, 2, 3, x, y);
        Plant cherryBomb = factory.createPlant(PlantType.CHERRY_BOMB, 2, 4, x, y);
        Plant lilyPad = factory.createPlant(PlantType.LILY_PAD, 3, 2, x, y);

        assertInstanceOf(Sunflower.class, sunflower);
        assertInstanceOf(Peashooter.class, peashooter);
        assertInstanceOf(SnowPea.class, snowPea);
        assertInstanceOf(WallNut.class, wallNut);
        assertInstanceOf(CherryBomb.class, cherryBomb);
        assertInstanceOf(LilyPad.class, lilyPad);

        // LilyPad sits on BASE layer; all other plants sit on TOP layer
        assertEquals(PlantLayer.BASE, lilyPad.getLayer());
        assertEquals(PlantLayer.TOP, sunflower.getLayer());
        assertEquals(PlantLayer.TOP, peashooter.getLayer());
        assertEquals(PlantLayer.TOP, snowPea.getLayer());
        assertEquals(PlantLayer.TOP, wallNut.getLayer());
        assertEquals(PlantLayer.TOP, cherryBomb.getLayer());
    }

    @Test
    void appliesJsonStatsToCreatedPlant() {
        PlantSpec wallNutSpec = factory.getSpec(PlantType.WALL_NUT);
        Plant wallNut = factory.createPlant(PlantType.WALL_NUT, 2, 5, 620, 300);

        assertEquals(wallNutSpec.getHp(), wallNut.getHp());
        assertEquals(wallNutSpec.getHp(), wallNut.getMaxHp());
        assertEquals(wallNutSpec.getCost(), wallNut.getCost());
        assertEquals(wallNutSpec.getCooldown(), wallNut.getCooldown());
        assertEquals(2, wallNut.getRow());
        assertEquals(5, wallNut.getCol());
        assertEquals(GameBoard.CELL_WIDTH, wallNut.getWidth());
        assertEquals(GameBoard.CELL_HEIGHT, wallNut.getHeight());
    }
}
