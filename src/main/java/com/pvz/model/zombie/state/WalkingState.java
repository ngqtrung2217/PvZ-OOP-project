package com.pvz.model.zombie.state;

import com.pvz.model.zombie.Zombie;

/**
 * Default state where a Zombie moves leftward along its lane.
 * Checks water terrain BEFORE plant target collisions:
 * - If on WATER and canSwim(), transitions to SwimmingState.
 * - If on WATER and !canSwim(), transitions to DrowningState.
 * - Otherwise, transitions to EatingState if an alive target is encountered.
 */
public class WalkingState implements ZombieState {

    @Override
    public void enter(Zombie zombie) {
        // Initialization when entering walking state
    }

    @Override
    public void update(Zombie zombie, double deltaTime) {
        // Move leftward based on zombie speed and elapsed time
        zombie.setX(zombie.getX() - zombie.getSpeed() * deltaTime);

        // 1. Check water terrain FIRST (before plant target collisions)
        if (zombie.isOnWater()) {
            if (zombie.canSwim()) {
                zombie.changeState(new SwimmingState());
            } else {
                zombie.changeState(new DrowningState());
            }
            return;
        }

        // 2. Check for living target
        if (zombie.getCurrentTarget() != null) {
            if (zombie.getCurrentTarget().isAlive()) {
                zombie.changeState(new EatingState());
                return;
            } else {
                zombie.setCurrentTarget(null);
            }
        }
    }

    @Override
    public void exit(Zombie zombie) {
        // Cleanup when leaving walking state
    }

    @Override
    public String getStateName() {
        return "WALKING";
    }
}
