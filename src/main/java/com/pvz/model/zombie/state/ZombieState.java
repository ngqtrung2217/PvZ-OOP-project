package com.pvz.model.zombie.state;

import com.pvz.model.zombie.Zombie;

/**
 * State interface for the Zombie Finite State Machine (FSM).
 * Handles entering, updating per frame, and exiting each state.
 */
public interface ZombieState {
    void enter(Zombie zombie);
    void update(Zombie zombie, double deltaTime);
    void exit(Zombie zombie);
    String getStateName();
}
