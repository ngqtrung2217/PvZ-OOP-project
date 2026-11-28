package com.pvz.model.economy;

import com.pvz.model.plant.PlantSpec;
import com.pvz.model.plant.PlantType;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.Objects;

/**
 * Single plant seed card in the HUD deck bar.
 * Tracks affordability, cooldown overlay ratio, and selection state.
 */
public class CardSlot {
    private final PlantType plantType;
    private final int cost;
    private final CooldownTimer cooldownTimer;

    private final double x;
    private final double y;
    private final double width;
    private final double height;

    private boolean selected = false;

    public CardSlot(PlantType plantType, PlantSpec spec,
                    double x, double y, double width, double height) {
        this.plantType = Objects.requireNonNull(plantType, "plantType must not be null");
        Objects.requireNonNull(spec, "spec must not be null");
        this.cost = spec.getCost();
        this.cooldownTimer = new CooldownTimer(spec.getCooldown());
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public PlantType getPlantType() {
        return plantType;
    }

    public int getCost() {
        return cost;
    }

    public CooldownTimer getCooldownTimer() {
        return cooldownTimer;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean canAfford(int currentSun) {
        return currentSun >= cost;
    }

    public boolean isReady() {
        return cooldownTimer.isReady();
    }

    public boolean isSelectable(int currentSun) {
        return isReady() && canAfford(currentSun);
    }

    public void triggerCooldown() {
        cooldownTimer.start();
        selected = false;
    }

    public Bounds getBounds() {
        return new BoundingBox(x, y, width, height);
    }

    public boolean contains(double mouseX, double mouseY) {
        return getBounds().contains(mouseX, mouseY);
    }

    public void update(double deltaTime) {
        cooldownTimer.update(deltaTime);
    }

    public void render(GraphicsContext gc, int currentSun) {
        // Card base
        gc.setFill(Color.web("#D7CCC8"));
        gc.fillRoundRect(x, y, width, height, 8, 8);

        // Border (gold when selected, dark brown otherwise)
        gc.setStroke(selected ? Color.web("#FFEB3B") : Color.web("#5D4037"));
        gc.setLineWidth(selected ? 3.0 : 2.0);
        gc.strokeRoundRect(x, y, width, height, 8, 8);

        // Plant short label
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(Color.web("#212121"));
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 10));
        gc.fillText(shortLabel(plantType), x + width / 2.0, y + height * 0.45);

        // Cost footer
        gc.setFill(Color.web("#FFF8E1"));
        gc.fillRect(x + 4, y + height - 20, width - 8, 16);
        gc.setFill(canAfford(currentSun) ? Color.web("#3E2723") : Color.web("#C62828"));
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        gc.fillText(String.valueOf(cost), x + width / 2.0, y + height - 7);

        // Insufficient sun dim overlay
        if (!canAfford(currentSun)) {
            gc.setFill(Color.web("#000000", 0.35));
            gc.fillRoundRect(x, y, width, height, 8, 8);
        }

        // Cooldown curtain overlay
        double progress = cooldownTimer.getProgress();
        if (progress > 0.0) {
            gc.setFill(Color.web("#000000", 0.55));
            gc.fillRect(x, y, width, height * progress);
        }
    }

    private static String shortLabel(PlantType type) {
        return switch (type) {
            case SUNFLOWER -> "Sunflower";
            case PEASHOOTER -> "Peashooter";
            case SNOW_PEA -> "SnowPea";
            case WALL_NUT -> "WallNut";
            case CHERRY_BOMB -> "Cherry";
            case LILY_PAD -> "LilyPad";
        };
    }
}
