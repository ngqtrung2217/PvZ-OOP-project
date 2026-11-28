package com.pvz.model.zombie;

import com.pvz.model.base.Entity;

/**
 * Abstraction for a 2-layer cell (TOP plant and BASE plant, such as a Peashooter on a LilyPad).
 * Allows zombies and {@link com.pvz.model.zombie.state.EatingState} to prioritize
 * eating the TOP layer before falling through to the BASE layer.
 */
public interface LayeredTarget {
    Entity getTopPlant();
    Entity getBasePlant();
}
