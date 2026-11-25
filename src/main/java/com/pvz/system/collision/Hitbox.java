package com.pvz.system.collision;

import com.pvz.model.base.GameObject;
import javafx.geometry.Bounds;

/**
 * Utility class providing Axis-Aligned Bounding Box (AABB) intersection checks
 * based on {@link GameObject#getHitbox()}.
 */
public final class Hitbox {

    private Hitbox() {
    }

    /**
     * Checks whether the AABB hitboxes of two {@link GameObject}s overlap.
     * Safely returns {@code false} if either object or hitbox is {@code null}.
     */
    public static boolean intersects(GameObject a, GameObject b) {
        if (a == null || b == null) {
            return false;
        }
        return intersects(a.getHitbox(), b.getHitbox());
    }

    /**
     * Checks whether two JavaFX {@link Bounds} overlap along both X and Y axes.
     * Safely returns {@code false} if either bound is {@code null}.
     */
    public static boolean intersects(Bounds a, Bounds b) {
        if (a == null || b == null) {
            return false;
        }
        return a.intersects(b);
    }

    /**
     * Checks whether a point {@code (px, py)} lies inside the object's hitbox.
     */
    public static boolean containsPoint(GameObject obj, double px, double py) {
        if (obj == null) {
            return false;
        }
        Bounds bounds = obj.getHitbox();
        return bounds != null && bounds.contains(px, py);
    }
}
