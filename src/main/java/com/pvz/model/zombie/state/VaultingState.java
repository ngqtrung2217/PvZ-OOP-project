package com.pvz.model.zombie.state;

import com.pvz.model.board.GameBoard;
import com.pvz.model.zombie.Zombie;

/**
 * State where a Zombie vaults over an obstacle.
 * Lasts 0.8 seconds, translates the zombie left by GameBoard.CELL_WIDTH (80.0 px),
 * marks hasVaulted = true, reduces speed to normal walking speed, and returns to WalkingState.
 */
public class VaultingState implements ZombieState {
    public static final double VAULT_DURATION = 0.8;
    public static final double NORMAL_WALK_SPEED = 17.0;

    private double vaultTimer = 0.0;
    private double startX = 0.0;

    @Override
    public void enter(Zombie zombie) {
        vaultTimer = 0.0;
        startX = zombie.getX();
        zombie.setCurrentTarget(null);
    }

    @Override
    public void update(Zombie zombie, double deltaTime) {
        vaultTimer += deltaTime;
        if (vaultTimer < VAULT_DURATION) {
            double progress = vaultTimer / VAULT_DURATION;
            zombie.setX(startX - progress * GameBoard.CELL_WIDTH);
        } else {
            zombie.setX(startX - GameBoard.CELL_WIDTH);
            zombie.setVaulted(true);
            zombie.setSpeed(NORMAL_WALK_SPEED);
            zombie.changeState(new WalkingState());
        }
    }

    @Override
    public void exit(Zombie zombie) {
        vaultTimer = 0.0;
    }

    @Override
    public String getStateName() {
        return "VAULTING";
    }

    public double getVaultTimer() {
        return vaultTimer;
    }
}
