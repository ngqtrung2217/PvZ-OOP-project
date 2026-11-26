package com.pvz.model.projectile;

import com.pvz.model.zombie.Zombie;
import com.pvz.system.resource.ResourceManager;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

/**
 * Frost pea projectile fired by SnowPeas that deals damage and slows zombies by 50%.
 */
public class SnowPeaProjectile extends Projectile {
    public static final double WIDTH = 20.0;
    public static final double HEIGHT = 20.0;
    public static final double DEFAULT_SPEED = 320.0;
    public static final int DEFAULT_DAMAGE = 20;
    public static final double DEFAULT_SLOW_MULTIPLIER = 0.5;
    public static final double DEFAULT_SLOW_DURATION = 4.0;
    public static final String SPRITE_PATH = "/assets/sprites/projectiles/ice_pea.png";

    private final double slowMultiplier;
    private final double slowDuration;

    public SnowPeaProjectile(double x, double y, int lane) {
        this(x, y, lane, DEFAULT_SPEED, DEFAULT_DAMAGE, DEFAULT_SLOW_MULTIPLIER, DEFAULT_SLOW_DURATION);
    }

    public SnowPeaProjectile(double x, double y, int lane, double speed, int damage,
                             double slowMultiplier, double slowDuration) {
        super(x, y, WIDTH, HEIGHT, speed, damage, lane, ProjectileType.SNOW_PEA);
        this.slowMultiplier = slowMultiplier;
        this.slowDuration = slowDuration;
    }

    @Override
    public boolean hasSlowEffect() {
        return true;
    }

    @Override
    public void onHit(Zombie target) {
        if (target != null && target.isAlive()) {
            target.takeDamage(damage);
            target.applySlow(slowMultiplier, slowDuration);
        }
        deactivate();
    }

    @Override
    public void render(GraphicsContext gc) {
        if (gc == null || !active) {
            return;
        }
        Image sprite = ResourceManager.getInstance().getImage(SPRITE_PATH);
        if (sprite != null) {
            gc.drawImage(sprite, x, y, width, height);
            return;
        }
        gc.setFill(Color.DEEPSKYBLUE);
        gc.fillOval(x, y, width, height);
        gc.setStroke(Color.LIGHTCYAN);
        gc.setLineWidth(1.5);
        gc.strokeOval(x, y, width, height);
    }

    public double getSlowMultiplier() {
        return slowMultiplier;
    }

    public double getSlowDuration() {
        return slowDuration;
    }
}
