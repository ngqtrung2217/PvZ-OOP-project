package com.pvz.model.zombie.state;

import com.pvz.model.zombie.Zombie;

/**
 * State representing a defeated zombie playing its death animation/delay.
 * Halts movement and counts down deathTimer (1.0 second) before setting readyToRemove to true.
 */
public class DeadState implements ZombieState {
    public static final double DEFAULT_DEATH_DURATION = 1.0;
    private double deathTimer = DEFAULT_DEATH_DURATION;

    @Override
    public void enter(Zombie zombie) {
        deathTimer = DEFAULT_DEATH_DURATION;
        zombie.setCurrentTarget(null);
    }

    @Override
    public void update(Zombie zombie, double deltaTime) {
        deathTimer -= deltaTime;
        if (deathTimer <= 0.0) {
            zombie.setReadyToRemove(true);
        }
    }

    @Override
    public void exit(Zombie zombie) {
        // No-op
    }

    @Override
    public String getStateName() {
        return "DEAD";
    }

    public double getDeathTimer() {
        return deathTimer;
    }
}
