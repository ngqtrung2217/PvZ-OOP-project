package com.pvz.system;

import com.pvz.model.base.Entity;
import com.pvz.model.projectile.PeaProjectile;
import com.pvz.model.projectile.Projectile;
import com.pvz.model.projectile.ProjectileType;
import com.pvz.model.projectile.SnowPeaProjectile;
import com.pvz.model.zombie.NormalZombie;
import com.pvz.model.zombie.PoleVaultingZombie;
import com.pvz.model.zombie.Zombie;
import com.pvz.model.zombie.state.DeadState;
import com.pvz.model.zombie.state.EatingState;
import com.pvz.model.zombie.state.VaultingState;
import com.pvz.system.collision.CollisionSystem;
import com.pvz.system.collision.Hitbox;
import com.pvz.system.event.BulletHitEvent;
import com.pvz.system.event.CombatEventBus;
import com.pvz.system.event.ZombieDeathEvent;
import javafx.scene.canvas.GraphicsContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Headless JUnit 5 tests for {@link Projectile}, {@link Hitbox}, and {@link CollisionSystem}.
 * Uses a local {@link StubObstacle} extending {@link Entity} to remain independent of Plant.
 */
class CollisionTest {

    /** Local stub Entity representing a plant/obstacle without importing com.pvz.model.plant. */
    private static class StubObstacle extends Entity {
        StubObstacle(double x, double y, double width, double height, int maxHp) {
            super(x, y, width, height, maxHp);
        }

        @Override
        public void update(double deltaTime) {
            // No-op stub
        }

        @Override
        public void render(GraphicsContext gc) {
            // No-op stub
        }
    }

    private CombatEventBus eventBus;
    private CollisionSystem collisionSystem;

    @BeforeEach
    void setUp() {
        eventBus = CombatEventBus.getInstance();
        eventBus.clear();
        collisionSystem = new CollisionSystem(eventBus);
    }

    @AfterEach
    void tearDown() {
        eventBus.clear();
    }

    @Test
    @DisplayName("PeaProjectile hitting a Zombie in the same lane deals damage, deactivates, and fires BulletHitEvent")
    void testPeaProjectileHitsZombieInSameLane() {
        NormalZombie zombie = new NormalZombie(500.0, 200.0, 1, null);
        int initialHp = zombie.getHp();
        PeaProjectile pea = new PeaProjectile(505.0, 220.0, 1);

        AtomicReference<BulletHitEvent> received = new AtomicReference<>();
        eventBus.subscribe(BulletHitEvent.class, received::set);

        collisionSystem.checkProjectileZombieCollisions(List.of(pea), List.of(zombie));

        assertEquals(initialHp - PeaProjectile.DEFAULT_DAMAGE, zombie.getHp());
        assertFalse(pea.isActive());
        assertTrue(pea.isReadyToRemove());
        assertEquals(ProjectileType.PEA, pea.getType());

        assertNotNull(received.get(), "BulletHitEvent must be published on impact");
        assertSame(pea, received.get().getProjectile());
        assertSame(zombie, received.get().getTarget());
        assertEquals(PeaProjectile.DEFAULT_DAMAGE, received.get().getDamage());
        assertFalse(received.get().hasSlowEffect());
    }

    @Test
    @DisplayName("Projectile and Zombie in different lanes do not collide even when X/Y bounds overlap")
    void testDifferentLanesDoNotCollide() {
        NormalZombie zombie = new NormalZombie(500.0, 200.0, 2, null);
        int initialHp = zombie.getHp();
        PeaProjectile pea = new PeaProjectile(505.0, 220.0, 0);

        List<BulletHitEvent> hits = new ArrayList<>();
        eventBus.subscribe(BulletHitEvent.class, hits::add);

        collisionSystem.checkProjectileZombieCollisions(List.of(pea), List.of(zombie));

        assertEquals(initialHp, zombie.getHp());
        assertTrue(pea.isActive());
        assertTrue(hits.isEmpty());
    }

    @Test
    @DisplayName("SnowPeaProjectile slows Zombie by 50% and speed recovers after slow duration expires")
    void testSnowPeaProjectileSlowsAndRecovers() {
        NormalZombie zombie = new NormalZombie(500.0, 200.0, 1, null);
        double speed = zombie.getSpeed();
        SnowPeaProjectile snowPea = new SnowPeaProjectile(505.0, 220.0, 1);

        AtomicReference<BulletHitEvent> hitEvent = new AtomicReference<>();
        eventBus.subscribe(BulletHitEvent.class, hitEvent::set);

        collisionSystem.checkProjectileZombieCollisions(List.of(snowPea), List.of(zombie));

        assertNotNull(hitEvent.get());
        assertTrue(hitEvent.get().hasSlowEffect());
        assertTrue(zombie.isSlowed());
        assertEquals(0.5, zombie.getSlowMultiplier(), 1e-9);

        // Move for 1.0s while slowed: should move half the normal distance
        double xBeforeSlowMove = zombie.getX();
        zombie.update(1.0);
        assertEquals(xBeforeSlowMove - speed * 0.5, zombie.getX(), 1e-6);

        // Exhaust remaining slow duration (3.0s left out of 4.0s)
        zombie.update(3.0);
        assertFalse(zombie.isSlowed());
        assertEquals(1.0, zombie.getSlowMultiplier(), 1e-9);

        // Move for 1.0s after recovery: should move full normal distance
        double xAfterRecovery = zombie.getX();
        zombie.update(1.0);
        assertEquals(xAfterRecovery - speed, zombie.getX(), 1e-6);
    }

    @Test
    @DisplayName("Killing a Zombie transitions to DeadState and publishes ZombieDeathEvent (KILLED) exactly once")
    void testLethalHitTransitionsToDeadStateAndFiresDeathEventOnce() {
        NormalZombie zombie = new NormalZombie(500.0, 200.0, 1, null);
        // Lethal pea dealing full zombie HP in one hit, plus a second overlapping pea
        PeaProjectile lethalPea = new PeaProjectile(505.0, 220.0, 1, 320.0, zombie.getHp());
        PeaProjectile followUpPea = new PeaProjectile(510.0, 220.0, 1, 320.0, 20);

        List<ZombieDeathEvent> deaths = new ArrayList<>();
        eventBus.subscribe(ZombieDeathEvent.class, deaths::add);

        collisionSystem.checkProjectileZombieCollisions(List.of(lethalPea, followUpPea), List.of(zombie));
        // Second collision pass must not re-fire death event
        collisionSystem.checkProjectileZombieCollisions(List.of(followUpPea), List.of(zombie));

        assertFalse(zombie.isAlive());
        assertInstanceOf(DeadState.class, zombie.getCurrentState());
        assertEquals(1, deaths.size(), "ZombieDeathEvent must fire exactly once");
        assertSame(zombie, deaths.get(0).getZombie());
        assertEquals(ZombieDeathEvent.DeathCause.KILLED, deaths.get(0).getCause());
        assertTrue(followUpPea.isActive(), "Second pea should not hit an already dead zombie");
    }

    @Test
    @DisplayName("NormalZombie colliding with stub Entity transitions to EatingState")
    void testNormalZombieCollidesWithStubEntity() {
        NormalZombie zombie = new NormalZombie(410.0, 200.0, 1, null);
        StubObstacle obstacle = new StubObstacle(380.0, 200.0, 60.0, 80.0, 300);

        collisionSystem.checkZombieEntityCollisionsInLane(List.of(zombie), List.of(obstacle), 1, false);

        assertInstanceOf(EatingState.class, zombie.getCurrentState());
        assertSame(obstacle, zombie.getCurrentTarget());
    }

    @Test
    @DisplayName("PoleVaultingZombie vaults over non-tall obstacle and eats tall obstacle")
    void testPoleVaultingZombieShortVsTallObstacle() {
        PoleVaultingZombie vaultingZombie = new PoleVaultingZombie(410.0, 200.0, 1, null);
        StubObstacle shortObstacle = new StubObstacle(380.0, 200.0, 60.0, 80.0, 300);

        collisionSystem.checkZombieEntityCollisionsInLane(List.of(vaultingZombie), List.of(shortObstacle), 1, false);
        assertInstanceOf(VaultingState.class, vaultingZombie.getCurrentState());

        PoleVaultingZombie blockedZombie = new PoleVaultingZombie(410.0, 300.0, 2, null);
        StubObstacle tallObstacle = new StubObstacle(380.0, 300.0, 60.0, 90.0, 800);

        collisionSystem.checkZombieEntityCollisionsInLane(List.of(blockedZombie), List.of(tallObstacle), 2, true);
        assertInstanceOf(EatingState.class, blockedZombie.getCurrentState());
        assertSame(tallObstacle, blockedZombie.getCurrentTarget());
    }

    @Test
    @DisplayName("Projectile moves rightward, deactivates off-screen, and Hitbox handles nulls safely")
    void testProjectileMovementOffScreenAndHitboxNullSafety() {
        PeaProjectile pea = new PeaProjectile(1000.0, 200.0, 0);
        assertDoesNotThrow(() -> pea.render(null));

        pea.update(0.2);
        assertEquals(1000.0 + PeaProjectile.DEFAULT_SPEED * 0.2, pea.getX(), 1e-6);
        assertTrue(pea.isActive());

        // Move past DEFAULT_MAX_X (1100.0)
        pea.update(0.5);
        assertFalse(pea.isActive());
        assertTrue(pea.isReadyToRemove());

        assertFalse(Hitbox.intersects((com.pvz.model.base.GameObject) null, pea));
        assertFalse(Hitbox.intersects(pea, null));
        assertFalse(Hitbox.containsPoint(null, 0, 0));
    }
}
