package com.pvz.model.zombie.state;

import com.pvz.model.zombie.Zombie;

/**
 * Default state where a Zombie moves leftward along its lane.
 * Transitions to EatingState if an alive target is encountered,
 * or SwimmingState if the zombie can swim and is currently on water.
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

        // Check for living target
        if (zombie.getCurrentTarget() != null) {
            if (zombie.getCurrentTarget().isAlive()) {
                zombie.changeState(new EatingState());
                return;
            } else {
                zombie.setCurrentTarget(null);
            }
        }

        // Check for water terrain transition
        if (zombie.isWaterZombie() && zombie.isOnWater()) {
            zombie.changeState(new SwimmingState());
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
