package com.pvz.model.plant;

import com.pvz.model.base.Entity;

/**
 * Abstract base class for all plants in the game.
 * Extends Entity (TV1) and encapsulates shared stats loaded from plants_spec.json.
 */
public abstract class Plant extends Entity {
    protected final PlantType type;
    protected final int cost;
    protected final double cooldown;
    protected final double attackSpeed;
    protected final int damage;
    protected final PlantLayer layer;

    protected final int row;
    protected final int col;

    protected Plant(PlantType type, int row, int col,
                    double x, double y, double width, double height,
                    PlantSpec spec) {
        super(x, y, width, height, spec.getHp());
        this.type = type;
        this.row = row;
        this.col = col;
        this.cost = spec.getCost();
        this.cooldown = spec.getCooldown();
        this.attackSpeed = spec.getAttackSpeed();
        this.damage = spec.getDamage();
        this.layer = spec.getLayer();
    }

    public PlantType getType() {
        return type;
    }

    public int getCost() {
        return cost;
    }

    public double getCooldown() {
        return cooldown;
    }

    public double getAttackSpeed() {
        return attackSpeed;
    }

    public int getDamage() {
        return damage;
    }

    public PlantLayer getLayer() {
        return layer;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public int getMaxHp() {
        return maxHp;
    }
}
