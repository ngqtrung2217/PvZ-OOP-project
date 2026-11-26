package com.pvz.plant;

import com.pvz.model.board.GameBoard;
import com.pvz.model.economy.Sun;
import com.pvz.model.economy.SunSpawner;
import com.pvz.model.plant.PlantFactory;
import com.pvz.model.plant.PlantType;
import com.pvz.model.plant.attack.Peashooter;
import com.pvz.model.plant.defense.WallNut;
import com.pvz.model.plant.instant.CherryBomb;
import com.pvz.model.plant.producer.Sunflower;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class PlantEconomyTest {
    private PlantFactory factory;

    @BeforeEach
    void setUp() {
        factory = new PlantFactory();
    }

    @Test
    void sunflowerProducesSunAccordingToSpecInterval() {
        Sunflower sunflower = (Sunflower) factory.createPlant(PlantType.SUNFLOWER, 1, 2, 380, 200);
        sunflower.setRandom(new Random(42L));
        double interval = factory.getSpec(PlantType.SUNFLOWER).getAttackSpeed();

        sunflower.update(interval - 0.1);
        assertTrue(sunflower.drainProducedSuns().isEmpty());

        sunflower.update(0.2);
        List<Sun> suns = sunflower.drainProducedSuns();
        assertEquals(1, suns.size());
        assertEquals(Sun.DEFAULT_VALUE, suns.get(0).getValue());
        assertTrue(suns.get(0).isLanded());
    }

    @Test
    void sunSpawnerIsDeterministicWithFixedSeedAndStaysInsideBoard() {
        SunSpawner spawnerA = new SunSpawner(10.0, new Random(12345L));
        SunSpawner spawnerB = new SunSpawner(10.0, new Random(12345L));

        List<Sun> sunsA = spawnerA.update(20.0);
        List<Sun> sunsB = spawnerB.update(20.0);

        assertEquals(2, sunsA.size());
        assertEquals(2, sunsB.size());
        for (int i = 0; i < 2; i++) {
            assertEquals(sunsA.get(i).getX(), sunsB.get(i).getX(), 1e-6);
            assertEquals(sunsA.get(i).getTargetY(), sunsB.get(i).getTargetY(), 1e-6);
            assertTrue(sunsA.get(i).getX() >= GameBoard.START_X);
            assertTrue(sunsA.get(i).getX() + Sun.DEFAULT_SIZE <= GameBoard.START_X + GameBoard.COLS * GameBoard.CELL_WIDTH);
            assertTrue(sunsA.get(i).getTargetY() >= GameBoard.START_Y);
            assertTrue(sunsA.get(i).getTargetY() + Sun.DEFAULT_SIZE <= GameBoard.START_Y + GameBoard.ROWS * GameBoard.CELL_HEIGHT);
        }
    }

    @Test
    void sunHitTestCollectsOnceAndExpiresAfterLifetime() {
        Sun sun = Sun.fromSky(300, 100, 160);
        sun.update(1.0); // Falls 60px -> lands at y=160
        assertTrue(sun.isLanded());
        assertTrue(sun.contains(310, 170));
        assertFalse(sun.contains(10, 10));

        assertEquals(25, sun.collect());
        assertEquals(0, sun.collect()); // Cannot collect twice
        assertFalse(sun.isActive());

        Sun expiringSun = Sun.fromSunflower(300, 200);
        expiringSun.update(Sun.DEFAULT_LIFETIME);
        assertTrue(expiringSun.isExpired());
        assertFalse(expiringSun.contains(310, 210));
    }

    @Test
    void peashooterFiresOnlyWhenZombieIsInLane() {
        Peashooter pea = (Peashooter) factory.createPlant(PlantType.PEASHOOTER, 2, 1, 300, 300);
        AtomicBoolean zombiePresent = new AtomicBoolean(false);
        pea.setZombieInLaneSupplier(zombiePresent::get);

        pea.update(5.0);
        assertEquals(0, pea.getShotsFired());

        zombiePresent.set(true);
        pea.update(0.1); // Immediately fires because it was primed while idle
        assertEquals(1, pea.getShotsFired());
    }

    @Test
    void wallNutTransitionsThroughThreeDamageStages() {
        WallNut wallNut = (WallNut) factory.createPlant(PlantType.WALL_NUT, 0, 4, 540, 100);
        assertEquals(0, wallNut.getDamageStage());

        wallNut.takeDamage(1500); // 2500 / 4000 -> stage 1
        assertEquals(1, wallNut.getDamageStage());

        wallNut.takeDamage(1500); // 1000 / 4000 -> stage 2
        assertEquals(2, wallNut.getDamageStage());
    }

    @Test
    void cherryBombExplodesIn3x3RangeAfterFuseDelay() {
        CherryBomb bomb = (CherryBomb) factory.createPlant(PlantType.CHERRY_BOMB, 2, 4, 540, 300);
        AtomicBoolean callbackFired = new AtomicBoolean(false);
        bomb.setOnExplode(b -> callbackFired.set(true));

        assertTrue(bomb.isInExplosionRange(1, 3));
        assertTrue(bomb.isInExplosionRange(3, 5));
        assertFalse(bomb.isInExplosionRange(0, 4));
        assertFalse(bomb.isInExplosionRange(2, 6));

        bomb.update(1.0);
        assertFalse(bomb.hasExploded());
        assertTrue(bomb.isAlive());

        bomb.update(0.25);
        assertTrue(bomb.hasExploded());
        assertFalse(bomb.isAlive());
        assertTrue(callbackFired.get());
    }
}
