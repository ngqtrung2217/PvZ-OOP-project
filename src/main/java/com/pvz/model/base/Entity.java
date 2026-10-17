package com.pvz.model.base;

/**
 * Abstract class representing alive game entities that possess hit points (HP).
 * Plants and Zombies extend this class.
 */
public abstract class Entity extends GameObject {
    protected int hp;
    protected int maxHp;

    public Entity(double x, double y, double width, double height, int maxHp) {
        super(x, y, width, height);
        this.maxHp = maxHp;
        this.hp = maxHp;
    }

    /**
     * Applies damage to this entity, decreasing HP down to a minimum of 0.
     * @param amount The damage amount to inflict.
     */
    public void takeDamage(int amount) {
        if (amount <= 0) {
            return;
        }
        this.hp = Math.max(0, this.hp - amount);
    }

    /**
     * Checks whether the entity is still alive.
     * @return true if hp > 0, false otherwise.
     */
    public boolean isAlive() {
        return this.hp > 0;
    }

    public int getHp() {
        return hp;
    }
}
