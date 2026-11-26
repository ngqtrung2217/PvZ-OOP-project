package com.pvz.model.zombie.state;

import com.pvz.model.base.Entity;
import com.pvz.model.zombie.LayeredTarget;
import com.pvz.model.zombie.Zombie;

/**
 * State where a Zombie stops and inflicts periodic bite damage on its target.
 * Supports 2-layer cells ({@link LayeredTarget}): eats TOP plant before falling
 * through to BASE plant (e.g., LilyPad). Once all plants on the cell are eaten,
 * checks current terrain: returns to SwimmingState on WATER (or DrowningState if
 * unable to swim), or WalkingState on land.
 */
public class EatingState implements ZombieState {
    private double eatTimer = 0.0;

    @Override
    public void enter(Zombie zombie) {
        eatTimer = 0.0;
        resolveLayeredTarget(zombie);
    }

    @Override
    public void update(Zombie zombie, double deltaTime) {
        Entity target = resolveLayeredTarget(zombie);
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
                Entity nextTarget = resolveLayeredTarget(zombie);
                if (nextTarget == null || !nextTarget.isAlive()) {
                    transitionBack(zombie);
                }
            }
        }
    }

    /**
     * Ensures the zombie targets the TOP plant of a LayeredTarget first,
     * falling back to the BASE plant only when TOP is null or dead.
     */
    private Entity resolveLayeredTarget(Zombie zombie) {
        Entity current = zombie.getCurrentTarget();
        LayeredTarget cell = zombie.getCurrentCellTarget();

        if (cell != null) {
            Entity top = cell.getTopPlant();
            if (top != null && top.isAlive()) {
                if (current != top) {
                    zombie.setCurrentTarget(top);
                }
                return top;
            }
            Entity base = cell.getBasePlant();
            if (base != null && base.isAlive()) {
                if (current != base) {
                    zombie.setCurrentTarget(base);
                }
                return base;
            }
            return null;
        }

        return current;
    }

    private void transitionBack(Zombie zombie) {
        zombie.setCurrentTarget(null);
        zombie.setCurrentCellTarget(null);
        if (zombie.isOnWater()) {
            if (zombie.canSwim()) {
                zombie.changeState(new SwimmingState());
            } else {
                zombie.changeState(new DrowningState());
            }
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
