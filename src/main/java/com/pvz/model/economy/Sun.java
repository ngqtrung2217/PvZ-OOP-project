package com.pvz.model.economy;

import com.pvz.model.base.GameObject;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/**
 * Collectible Sun resource spawned either from the sky (SunSpawner) or a Sunflower.
 * Remains collectible even when landing on a WaterTile.
 */
public class Sun extends GameObject {
    public static final int DEFAULT_VALUE = 25;
    public static final double DEFAULT_SIZE = 44.0;
    public static final double DEFAULT_FALL_SPEED = 60.0;
    public static final double DEFAULT_LIFETIME = 10.0;

    private final int value;
    private final double targetY;
    private final double fallSpeed;
    private final double lifetime;

    private double landedAge = 0.0;
    private boolean collected = false;
    private boolean expired = false;

    public Sun(double x, double startY, double targetY, int value, double fallSpeed, double lifetime) {
        super(x, startY, DEFAULT_SIZE, DEFAULT_SIZE);
        this.targetY = Math.max(startY, targetY);
        this.value = value;
        this.fallSpeed = Math.max(0.0, fallSpeed);
        this.lifetime = Math.max(0.0, lifetime);
    }

    /**
     * Creates a Sun falling from the top of the screen down to targetY.
     */
    public static Sun fromSky(double x, double startY, double targetY) {
        return new Sun(x, startY, targetY, DEFAULT_VALUE, DEFAULT_FALL_SPEED, DEFAULT_LIFETIME);
    }

    /**
     * Creates a Sun produced directly at a Sunflower's position.
     */
    public static Sun fromSunflower(double x, double y) {
        return new Sun(x, y, y, DEFAULT_VALUE, 0.0, DEFAULT_LIFETIME);
    }

    /**
     * Bounding-box hit test for mouse clicks.
     */
    public boolean contains(double mouseX, double mouseY) {
        return isActive() && getHitbox().contains(mouseX, mouseY);
    }

    /**
     * Marks this Sun as collected and returns its Sun value (0 if already collected or expired).
     */
    public int collect() {
        if (!isActive()) {
            return 0;
        }
        collected = true;
        return value;
    }

    public boolean isLanded() {
        return y >= targetY;
    }

    public boolean isCollected() {
        return collected;
    }

    public boolean isExpired() {
        return expired;
    }

    public boolean isActive() {
        return !collected && !expired;
    }

    public int getValue() {
        return value;
    }

    public double getTargetY() {
        return targetY;
    }

    @Override
    public void update(double deltaTime) {
        if (!isActive() || deltaTime <= 0) {
            return;
        }
        if (y < targetY) {
            y = Math.min(targetY, y + fallSpeed * deltaTime);
        } else {
            landedAge += deltaTime;
            if (landedAge >= lifetime) {
                expired = true;
            }
        }
    }

    @Override
    public void render(GraphicsContext gc) {
        if (!isActive()) {
            return;
        }
        // Outer glow
        gc.setFill(Color.web("#FFF59D", 0.55));
        gc.fillOval(x - 4, y - 4, width + 8, height + 8);

        // Core sun disk
        gc.setFill(Color.web("#FDD835"));
        gc.fillOval(x, y, width, height);

        gc.setStroke(Color.web("#F57F17"));
        gc.setLineWidth(2.0);
        gc.strokeOval(x, y, width, height);

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(Color.web("#5D4037"));
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        gc.fillText(String.valueOf(value), x + width / 2.0, y + height / 2.0 + 5);
    }
}
