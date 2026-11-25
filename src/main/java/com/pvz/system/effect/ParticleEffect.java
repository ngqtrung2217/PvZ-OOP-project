package com.pvz.system.effect;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Represents a short-lived visual particle effect rendered on the game canvas
 * (such as bomb explosions, pea splats, frost slows, or zombie defeats).
 */
public class ParticleEffect {

    public enum EffectType {
        EXPLOSION,
        PEA_SPLAT,
        ICE_SLOW,
        ZOMBIE_DEATH
    }

    private final double x;
    private final double y;
    private final double duration;
    private final double radius;
    private final EffectType type;
    private double elapsedTime;

    public ParticleEffect(double x, double y, double duration, double radius, EffectType type) {
        this.x = x;
        this.y = y;
        this.duration = Math.max(0.01, duration);
        this.radius = Math.max(1.0, radius);
        this.type = type != null ? type : EffectType.PEA_SPLAT;
        this.elapsedTime = 0.0;
    }

    /**
     * Advances the effect's lifetime by {@code deltaTime} seconds.
     */
    public void update(double deltaTime) {
        if (deltaTime > 0.0) {
            elapsedTime += deltaTime;
        }
    }

    /**
     * Renders the fading particle effect onto the provided {@link GraphicsContext}.
     * Safely no-ops if {@code gc} is {@code null} or the effect has finished.
     */
    public void render(GraphicsContext gc) {
        if (gc == null || isFinished()) {
            return;
        }

        double progress = Math.min(1.0, elapsedTime / duration);
        double alpha = Math.max(0.0, 1.0 - progress);
        double currentRadius = radius * (0.35 + 0.65 * progress);

        gc.save();
        gc.setGlobalAlpha(alpha);

        switch (type) {
            case EXPLOSION -> {
                gc.setFill(Color.ORANGERED);
                gc.fillOval(x - currentRadius, y - currentRadius, currentRadius * 2.0, currentRadius * 2.0);
                double innerRadius = currentRadius * 0.6;
                gc.setFill(Color.GOLD);
                gc.fillOval(x - innerRadius, y - innerRadius, innerRadius * 2.0, innerRadius * 2.0);
            }
            case PEA_SPLAT -> {
                gc.setFill(Color.LIMEGREEN);
                drawBurstDots(gc, currentRadius, 5, 4.0);
            }
            case ICE_SLOW -> {
                gc.setStroke(Color.DEEPSKYBLUE);
                gc.setLineWidth(2.5);
                gc.strokeOval(x - currentRadius, y - currentRadius, currentRadius * 2.0, currentRadius * 2.0);
                gc.setFill(Color.LIGHTCYAN);
                drawBurstDots(gc, currentRadius * 0.85, 6, 3.5);
            }
            case ZOMBIE_DEATH -> {
                gc.setFill(Color.SLATEGRAY);
                gc.fillOval(x - currentRadius, y - currentRadius, currentRadius * 2.0, currentRadius * 2.0);
            }
        }

        gc.restore();
    }

    private void drawBurstDots(GraphicsContext gc, double dist, int count, double dotRadius) {
        for (int i = 0; i < count; i++) {
            double angle = (2.0 * Math.PI * i) / count;
            double dx = x + Math.cos(angle) * dist;
            double dy = y + Math.sin(angle) * dist;
            gc.fillOval(dx - dotRadius, dy - dotRadius, dotRadius * 2.0, dotRadius * 2.0);
        }
    }

    public boolean isFinished() {
        return elapsedTime >= duration;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getDuration() {
        return duration;
    }

    public double getElapsedTime() {
        return elapsedTime;
    }

    public double getRadius() {
        return radius;
    }

    public EffectType getType() {
        return type;
    }
}
