package com.pvz.model.plant;

/**
 * Data holder for a plant's stats loaded from plants_spec.json via Gson.
 */
public class PlantSpec {
    private int hp;
    private int cost;
    private double cooldown;
    private double attackSpeed;
    private int damage;
    private PlantLayer layer;

    public PlantSpec() {
        // Required no-arg constructor for Gson deserialization
    }

    public PlantSpec(int hp, int cost, double cooldown, double attackSpeed, int damage, PlantLayer layer) {
        this.hp = hp;
        this.cost = cost;
        this.cooldown = cooldown;
        this.attackSpeed = attackSpeed;
        this.damage = damage;
        this.layer = layer;
    }

    public int getHp() {
        return hp;
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
}
