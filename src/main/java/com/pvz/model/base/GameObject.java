package com.pvz.model.base;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.canvas.GraphicsContext;

/**
 * Root abstract class representing any visual or interactive element in the game.
 * Contains position, dimensions, hitbox calculation, and lifecycle methods.
 */
public abstract class GameObject {
    protected double x;
    protected double y;
    protected double width;
    protected double height;

    public GameObject(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     * Updates the internal state of the object per frame.
     * @param deltaTime Elapsed time since the previous frame in seconds.
     */
    public abstract void update(double deltaTime);

    /**
     * Renders the visual representation of this object onto the JavaFX Canvas.
     * @param gc The GraphicsContext used for drawing.
     */
    public abstract void render(GraphicsContext gc);

    /**
     * Calculates the Axis-Aligned Bounding Box (AABB) for collision detection.
     * @return BoundingBox representing the object's hitbox boundaries.
     */
    public Bounds getHitbox() {
        return new BoundingBox(x, y, width, height);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }
}
