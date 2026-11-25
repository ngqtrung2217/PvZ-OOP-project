package com.pvz.system.event;

/**
 * Event published when the player collects a sun resource.
 */
public final class SunCollectedEvent extends GameEvent {
    private final int amount;
    private final double x;
    private final double y;

    public SunCollectedEvent(int amount, double x, double y) {
        super();
        this.amount = amount;
        this.x = x;
        this.y = y;
    }

    public int getAmount() {
        return amount;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }
}
