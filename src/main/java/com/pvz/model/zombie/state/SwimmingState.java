package com.pvz.model.zombie.state;

import com.pvz.model.zombie.Zombie;

/**
 * State where a Zombie traverses water tiles using its swim speed.
 * Transitions to EatingState if encountering an alive target,
 * or back to WalkingState when stepping onto land (grass).
 */
public class SwimmingState implements ZombieState {

    @Override
    public void enter(Zombie zombie) {
        // Initialization when entering swimming state
    }

    @Override
    public void update(Zombie zombie, double deltaTime) {
        // Move leftward using swim speed
        zombie.setX(zombie.getX() - zombie.getSwimSpeed() * deltaTime);

        // Check for living target
        if (zombie.getCurrentTarget() != null) {
            if (zombie.getCurrentTarget().isAlive()) {
                zombie.changeState(new EatingState());
                return;
            } else {
                zombie.setCurrentTarget(null);
            }
        }

        // Check if no longer in water
        if (!zombie.isOnWater()) {
            zombie.changeState(new WalkingState());
        }
    }

    @Override
    public void exit(Zombie zombie) {
        // Cleanup when leaving swimming state
    }

    @Override
    public String getStateName() {
        return "SWIMMING";
    }
}
