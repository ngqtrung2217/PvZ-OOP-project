package com.pvz.model.zombie.state;

import com.pvz.model.zombie.Zombie;
import com.pvz.system.event.CombatEventBus;
import com.pvz.system.event.ZombieDeathEvent;

/**
 * State entered when a non-swimming zombie steps into a WATER tile.
 * Halts horizontal movement, counts down the drowning duration, marks the zombie
 * as drowned, publishes {@link ZombieDeathEvent.DeathCause#DROWNED}, and transitions to DeadState.
 */
public class DrowningState implements ZombieState {
    public static final double DEFAULT_DROWN_DURATION = 1.0;

    private double drownTimer = DEFAULT_DROWN_DURATION;
    private boolean eventPublished = false;

    @Override
    public void enter(Zombie zombie) {
        drownTimer = DEFAULT_DROWN_DURATION;
        eventPublished = false;
        zombie.setCurrentTarget(null);
        zombie.setCurrentCellTarget(null);
    }

    @Override
    public void update(Zombie zombie, double deltaTime) {
        drownTimer -= deltaTime;
        if (drownTimer <= 0.0) {
            zombie.setDrowned(true);
            if (!eventPublished && zombie.isAlive()) {
                eventPublished = true;
                CombatEventBus.getInstance().publish(
                        new ZombieDeathEvent(zombie, ZombieDeathEvent.DeathCause.DROWNED)
                );
            }
            if (zombie.isAlive()) {
                zombie.takeDamage(Integer.MAX_VALUE);
            }
            if (!(zombie.getCurrentState() instanceof DeadState)) {
                zombie.changeState(new DeadState());
            }
        }
    }

    @Override
    public void exit(Zombie zombie) {
        // No-op
    }

    @Override
    public String getStateName() {
        return "DROWNING";
    }

    public double getDrownTimer() {
        return drownTimer;
    }
}
