package com.pvz.model.projectile;

import com.pvz.model.base.GameObject;
import com.pvz.model.zombie.Zombie;
import javafx.scene.canvas.GraphicsContext;

/**
 * Abstract base class for all projectiles traveling horizontally across a lane.
 */
public abstract class Projectile extends GameObject {
    public static final double DEFAULT_MAX_X = 1100.0;

    protected double speed;
    protected int damage;
    protected int lane;
    protected boolean active;
    protected final ProjectileType type;
    protected double maxX = DEFAULT_MAX_X;

    protected Projectile(double x, double y, double width, double height,
                         double speed, int damage, int lane, ProjectileType type) {
        super(x, y, width, height);
        this.speed = speed;
        this.damage = damage;
        this.lane = lane;
        this.type = type;
        this.active = true;
    }

    @Override
    public void update(double deltaTime) {
        if (!active || deltaTime <= 0.0) {
            return;
        }
        x += speed * deltaTime;
        if (x > maxX || x + width < 0.0) {
            active = false;
        }
    }

    /**
     * Applies this projectile's hit effects (damage, status effects) to the target zombie
     * and deactivates the projectile.
     */
    public abstract void onHit(Zombie target);

    /**
     * Returns {@code true} if this projectile applies a slow/frost effect on impact.
     */
    public boolean hasSlowEffect() {
        return false;
    }

    @Override
    public abstract void render(GraphicsContext gc);

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public int getDamage() {
        return damage;
    }

    public void setDamage(int damage) {
        this.damage = damage;
    }

    public int getLane() {
        return lane;
    }

    public void setLane(int lane) {
        this.lane = lane;
    }

    public ProjectileType getType() {
        return type;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void deactivate() {
        this.active = false;
    }

    public boolean isReadyToRemove() {
        return !active;
    }

    public double getMaxX() {
        return maxX;
    }

    public void setMaxX(double maxX) {
        this.maxX = maxX;
    }
}
