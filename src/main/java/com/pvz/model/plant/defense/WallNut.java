package com.pvz.model.plant.defense;

import com.pvz.model.plant.Plant;
import com.pvz.model.plant.PlantSpec;
import com.pvz.model.plant.PlantType;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * High-HP defensive barrier plant that visually degrades as it takes damage.
 */
public class WallNut extends Plant {

    public WallNut(int row, int col, double x, double y, double width, double height, PlantSpec spec) {
        super(PlantType.WALL_NUT, row, col, x, y, width, height, spec);
    }

    /**
     * Returns visual degradation stage based on remaining HP ratio:
     * 0 = healthy (> 2/3 HP), 1 = cracked (> 1/3 HP), 2 = heavily cracked (<= 1/3 HP).
     */
    public int getDamageStage() {
        if (maxHp <= 0) {
            return 0;
        }
        double ratio = (double) hp / maxHp;
        if (ratio > 2.0 / 3.0) {
            return 0;
        }
        if (ratio > 1.0 / 3.0) {
            return 1;
        }
        return 2;
    }

    @Override
    public void update(double deltaTime) {
        // Passive defensive wall; no per-frame timer needed
    }

    @Override
    public void render(GraphicsContext gc) {
        int stage = getDamageStage();
        Color bodyColor = switch (stage) {
            case 0 -> Color.web("#A1887F");
            case 1 -> Color.web("#8D6E63");
            default -> Color.web("#6D4C41");
        };

        gc.setFill(bodyColor);
        gc.fillOval(x + width * 0.16, y + height * 0.14, width * 0.68, height * 0.72);

        gc.setStroke(Color.web("#4E342E"));
        gc.setLineWidth(2.0);
        gc.strokeOval(x + width * 0.16, y + height * 0.14, width * 0.68, height * 0.72);

        // Eyes
        gc.setFill(Color.WHITE);
        gc.fillOval(x + width * 0.32, y + height * 0.34, width * 0.12, height * 0.10);
        gc.fillOval(x + width * 0.54, y + height * 0.34, width * 0.12, height * 0.10);
        gc.setFill(Color.BLACK);
        gc.fillOval(x + width * 0.36, y + height * 0.36, width * 0.05, height * 0.05);
        gc.fillOval(x + width * 0.58, y + height * 0.36, width * 0.05, height * 0.05);

        // Cracks for stage 1 and 2
        if (stage >= 1) {
            gc.setStroke(Color.web("#3E2723"));
            gc.setLineWidth(2.0);
            gc.strokeLine(x + width * 0.45, y + height * 0.18, x + width * 0.52, y + height * 0.32);
        }
        if (stage >= 2) {
            gc.strokeLine(x + width * 0.30, y + height * 0.55, x + width * 0.48, y + height * 0.68);
        }
    }
}
