package com.pvz.model.projectile;

import com.pvz.model.zombie.Zombie;
import com.pvz.system.resource.ResourceManager;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

/**
 * Standard green pea projectile fired by Peashooters.
 */
public class PeaProjectile extends Projectile {
    public static final double WIDTH = 20.0;
    public static final double HEIGHT = 20.0;
    public static final double DEFAULT_SPEED = 320.0;
    public static final int DEFAULT_DAMAGE = 20;
    public static final String SPRITE_PATH = "/assets/sprites/projectiles/pea.png";

    public PeaProjectile(double x, double y, int lane) {
        this(x, y, lane, DEFAULT_SPEED, DEFAULT_DAMAGE);
    }

    public PeaProjectile(double x, double y, int lane, double speed, int damage) {
        super(x, y, WIDTH, HEIGHT, speed, damage, lane, ProjectileType.PEA);
    }

    @Override
    public void onHit(Zombie target) {
        if (target != null && target.isAlive()) {
            target.takeDamage(damage);
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
        gc.setFill(Color.LIMEGREEN);
        gc.fillOval(x, y, width, height);
        gc.setStroke(Color.DARKGREEN);
        gc.setLineWidth(1.5);
        gc.strokeOval(x, y, width, height);
    }
}
