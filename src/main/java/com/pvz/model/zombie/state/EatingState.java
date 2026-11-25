package com.pvz.model.zombie.state;

import com.pvz.model.base.Entity;
import com.pvz.model.zombie.Zombie;

/**
 * State where a Zombie stops and inflicts periodic bite damage on its target.
 * Transitions back to WalkingState (or SwimmingState on water) once the target is destroyed.
 */
public class EatingState implements ZombieState {
    private double eatTimer = 0.0;

    @Override
    public void enter(Zombie zombie) {
        eatTimer = 0.0;
    }

    @Override
    public void update(Zombie zombie, double deltaTime) {
        Entity target = zombie.getCurrentTarget();
        if (target == null || !target.isAlive()) {
            transitionBack(zombie);
            return;
        }

        eatTimer += deltaTime;
        double interval = zombie.getAttackInterval();
        if (eatTimer >= interval) {
            target.takeDamage(zombie.getAttackDamage());
            eatTimer = 0.0;

            if (!target.isAlive()) {
                transitionBack(zombie);
            }
        }
    }

    private void transitionBack(Zombie zombie) {
        zombie.setCurrentTarget(null);
        if (zombie.isWaterZombie() && zombie.isOnWater()) {
            zombie.changeState(new SwimmingState());
        } else {
            zombie.changeState(new WalkingState());
        }
    }

    @Override
    public void exit(Zombie zombie) {
        eatTimer = 0.0;
    }

    @Override
    public String getStateName() {
        return "EATING";
    }

    public double getEatTimer() {
        return eatTimer;
    }
}
