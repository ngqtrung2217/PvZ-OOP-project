package com.pvz.system;

import com.pvz.model.board.GameBoard;
import com.pvz.model.board.Lane;
import com.pvz.model.zombie.ConeheadZombie;
import com.pvz.model.zombie.NormalZombie;
import com.pvz.model.zombie.Zombie;
import com.pvz.model.zombie.state.DeadState;
import com.pvz.system.collision.CollisionSystem;
import com.pvz.system.defense.LawnMower;
import com.pvz.system.event.CombatEventBus;
import com.pvz.system.event.ZombieDeathEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Headless JUnit 5 tests for {@link LawnMower} and LawnMower-Zombie collision handling.
 */
class LawnMowerTest {

    private CombatEventBus eventBus;
    private CollisionSystem collisionSystem;
    private GameBoard board;

    @BeforeEach
    void setUp() {
        eventBus = CombatEventBus.getInstance();
        eventBus.clear();
        collisionSystem = new CollisionSystem(eventBus);
        board = new GameBoard();
    }

    @AfterEach
    void tearDown() {
        eventBus.clear();
    }

    @Test
    @DisplayName("LawnMower starts READY and syncs with Lane.triggerLawnMower() when activated")
    void testInitialStateAndLaneSync() {
        Lane lane = board.getLane(1);
        LawnMower mower = new LawnMower(1, lane);

        assertTrue(mower.isReady());
        assertFalse(mower.isActive());
        assertFalse(mower.isUsed());
        assertFalse(mower.isReadyToRemove());
        assertTrue(lane.isLawnMowerReady());
        assertEquals(1, mower.getLane());

        mower.activate();

        assertFalse(mower.isReady());
        assertTrue(mower.isActive());
        assertFalse(lane.isLawnMowerReady(), "Activating mower must trigger Lane.triggerLawnMower()");
        assertDoesNotThrow(() -> mower.render(null));
    }

    @Test
    @DisplayName("Zombie collision in same lane activates mower, kills normal & armored zombies, and fires MOWED events")
    void testLawnMowerClearsLaneIncludingArmoredZombie() {
        int targetLane = 2;
        LawnMower mower = new LawnMower(targetLane, board);
        double laneY = board.getCellOriginY(targetLane);

        // First zombie touches mower at left edge; armored ConeheadZombie stands further down the same lane
        NormalZombie frontZombie = new NormalZombie(mower.getX() + 20.0, laneY, targetLane, board);
        ConeheadZombie armoredZombie = new ConeheadZombie(450.0, laneY, targetLane, board);
        List<Zombie> zombies = List.of(frontZombie, armoredZombie);

        List<ZombieDeathEvent> deathEvents = new ArrayList<>();
        eventBus.subscribe(ZombieDeathEvent.class, deathEvents::add);

        // First contact activates mower and mows down frontZombie
        collisionSystem.checkLawnMowerZombieCollisions(List.of(mower), zombies);
        assertTrue(mower.isActive());
        assertFalse(frontZombie.isAlive());
        assertInstanceOf(DeadState.class, frontZombie.getCurrentState());
        assertTrue(armoredZombie.isAlive(), "Armored zombie further right is not hit yet");
        assertEquals(1, deathEvents.size());
        assertEquals(ZombieDeathEvent.DeathCause.MOWED, deathEvents.get(0).getCause());

        // Advance mower across the lane until it hits armoredZombie
        for (int i = 0; i < 40 && mower.isActive(); i++) {
            mower.update(0.05);
            collisionSystem.checkLawnMowerZombieCollisions(List.of(mower), zombies);
        }

        assertFalse(armoredZombie.isAlive(), "ConeheadZombie must be eliminated despite armor HP");
        assertEquals(0, armoredZombie.getArmorHp());
        assertInstanceOf(DeadState.class, armoredZombie.getCurrentState());
        assertEquals(2, deathEvents.size(), "Each mowed zombie should fire ZombieDeathEvent exactly once");
        assertEquals(ZombieDeathEvent.DeathCause.MOWED, deathEvents.get(1).getCause());
        assertSame(armoredZombie, deathEvents.get(1).getZombie());
    }

    @Test
    @DisplayName("Zombie in a different lane does not trigger or get hit by LawnMower")
    void testDifferentLaneDoesNotTriggerMower() {
        LawnMower mower = new LawnMower(0, board);
        NormalZombie otherLaneZombie = new NormalZombie(mower.getX() + 10.0, mower.getY(), 3, board);

        List<ZombieDeathEvent> deathEvents = new ArrayList<>();
        eventBus.subscribe(ZombieDeathEvent.class, deathEvents::add);

        collisionSystem.checkLawnMowerZombieCollisions(List.of(mower), List.of(otherLaneZombie));

        assertTrue(mower.isReady(), "Mower on lane 0 must stay READY for zombie on lane 3");
        assertTrue(otherLaneZombie.isAlive());
        assertTrue(deathEvents.isEmpty());
    }

    @Test
    @DisplayName("Active LawnMower transitions to USED once it passes maxX")
    void testMowerTransitionsToUsedPastMaxX() {
        LawnMower mower = new LawnMower(0, board);
        mower.activate();
        assertTrue(mower.isActive());

        // Move far enough to exceed DEFAULT_MAX_X (1024.0)
        mower.update(5.0);

        assertTrue(mower.isUsed());
        assertTrue(mower.isReadyToRemove());
        assertFalse(mower.isActive());
        assertDoesNotThrow(() -> mower.render(null));
    }
}
