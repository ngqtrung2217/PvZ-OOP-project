package com.pvz.model.zombie;

import com.pvz.model.base.Entity;
import com.pvz.model.board.GameBoard;
import com.pvz.model.board.TerrainQuery;
import com.pvz.model.tile.TileType;
import com.pvz.model.zombie.state.DeadState;
import com.pvz.model.zombie.state.DrowningState;
import com.pvz.model.zombie.state.EatingState;
import com.pvz.model.zombie.state.WalkingState;
import com.pvz.model.zombie.state.ZombieState;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Abstract base class for all Zombie variants.
 * Managed by a Finite State Machine (FSM) via {@link ZombieState}.
 */
public abstract class Zombie extends Entity {
    protected int lane;
    protected double speed;
    protected int attackDamage;
    protected ZombieState currentState;
    protected Entity currentTarget;
    protected boolean readyToRemove;
    protected GameBoard board;
    protected Color renderColor;

    // Additional configuration fields for state transitions & mechanics
    protected double attackInterval = 1.0;
    protected double swimSpeed;
    protected boolean hasVaulted = false;
    protected boolean waterZombie = false;
    protected boolean canSwim = false;
    protected boolean drowned = false;
    protected TerrainQuery terrainQuery;
    protected LayeredTarget currentCellTarget;
    /** Spec type such as "NORMAL" or "BUCKETHEAD"; set by ZombieFactory, used for scoring. */
    private String type = "UNKNOWN";
    protected double slowMultiplier = 1.0;
    protected double slowTimer = 0.0;

    public Zombie(double x, double y, double width, double height, int maxHp,
                  double speed, int attackDamage, int lane, GameBoard board) {
        super(x, y, width, height, maxHp);
        this.speed = speed;
        this.attackDamage = attackDamage;
        this.swimSpeed = speed;
        this.lane = lane;
        this.board = board;
        this.terrainQuery = board;
        this.readyToRemove = false;
        this.renderColor = Color.DARKGRAY;
        this.currentState = new WalkingState();
        this.currentState.enter(this);
    }

    public Zombie(double x, double y, double width, double height, int maxHp,
                  double speed, int attackDamage) {
        this(x, y, width, height, maxHp, speed, attackDamage, 0, null);
    }

    public Zombie(double x, double y, double width, double height, int maxHp) {
        this(x, y, width, height, maxHp, 17.0, 25, 0, null);
    }

    /**
     * Transitions this zombie to a new state, invoking exit() on the old
     * state and enter() on the new one.
     *
     * @param newState The incoming ZombieState.
     */
    public void changeState(ZombieState newState) {
        if (newState == null) {
            return;
        }
        if (this.currentState != null) {
            this.currentState.exit(this);
        }
        this.currentState = newState;
        this.currentState.enter(this);
    }

    /**
     * Polymorphic hook called when colliding with an obstacle or plant.
     * Default behavior sets the target and transitions into EatingState.
     * Subclasses (such as PoleVaultingZombie) may override to vault over obstacles.
     *
     * @param target The collided entity.
     * @param isTallObstacle True if obstacle cannot be vaulted over.
     */
    public void onCollideWithTarget(Entity target, boolean isTallObstacle) {
        if (target == null || !target.isAlive()) {
            return;
        }
        if (currentState instanceof DeadState || currentState instanceof DrowningState) {
            return;
        }
        if (isOnWater() && !canSwim()) {
            changeState(new DrowningState());
            return;
        }
        this.currentTarget = target;
        changeState(new EatingState());
    }

    /**
     * Collision hook for 2-layer cells (TOP and BASE plants).
     * Prioritizes TOP layer before falling back to BASE layer.
     *
     * @param cell           The 2-layer cell target.
     * @param isTallObstacle True if the active target cannot be vaulted over.
     */
    public void onCollideWithCell(LayeredTarget cell, boolean isTallObstacle) {
        if (cell == null) {
            return;
        }
        if (currentState instanceof DeadState || currentState instanceof DrowningState) {
            return;
        }
        if (isOnWater() && !canSwim()) {
            changeState(new DrowningState());
            return;
        }
        Entity target = cell.getTopPlant();
        if (target == null || !target.isAlive()) {
            target = cell.getBasePlant();
        }
        if (target != null && target.isAlive()) {
            this.currentCellTarget = cell;
            onCollideWithTarget(target, isTallObstacle);
        }
    }

    @Override
    public void takeDamage(int amount) {
        super.takeDamage(amount);
        if (!isAlive() && !(currentState instanceof DeadState)) {
            changeState(new DeadState());
        }
    }

    @Override
    public void update(double deltaTime) {
        double effectiveDelta = deltaTime;
        if (slowTimer > 0.0 && deltaTime > 0.0) {
            if (deltaTime <= slowTimer) {
                effectiveDelta = deltaTime * slowMultiplier;
                slowTimer -= deltaTime;
            } else {
                effectiveDelta = slowTimer * slowMultiplier + (deltaTime - slowTimer);
                slowTimer = 0.0;
                slowMultiplier = 1.0;
            }
            if (slowTimer <= 0.0) {
                slowTimer = 0.0;
                slowMultiplier = 1.0;
            }
        }
        if (currentState != null) {
            currentState.update(this, effectiveDelta);
        }
    }

    /**
     * Applies a temporary slow effect (e.g. from SnowPeaProjectile) to this zombie.
     *
     * @param multiplier Speed multiplier in (0.0, 1.0], e.g. 0.5 for 50% slow.
     * @param duration   Duration of the slow effect in seconds.
     */
    public void applySlow(double multiplier, double duration) {
        if (duration <= 0.0 || Double.isNaN(multiplier) || Double.isNaN(duration)) {
            return;
        }
        double clampedMultiplier = Math.max(0.1, Math.min(1.0, multiplier));
        if (slowTimer <= 0.0) {
            this.slowMultiplier = clampedMultiplier;
        } else {
            this.slowMultiplier = Math.min(this.slowMultiplier, clampedMultiplier);
        }
        this.slowTimer = Math.max(this.slowTimer, duration);
    }

    public boolean isSlowed() {
        return slowTimer > 0.0 && slowMultiplier < 1.0;
    }

    public double getSlowMultiplier() {
        return isSlowed() ? slowMultiplier : 1.0;
    }

    public double getSlowTimer() {
        return slowTimer;
    }

    public void clearSlow() {
        this.slowTimer = 0.0;
        this.slowMultiplier = 1.0;
    }

    @Override
    public void render(GraphicsContext gc) {
        if (gc == null) {
            return;
        }

        // Draw fallback body rectangle
        gc.setFill(renderColor != null ? renderColor : Color.DARKGRAY);
        gc.fillRect(x, y, width, height);

        // Body outline
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1.0);
        gc.strokeRect(x, y, width, height);

        // Draw health bar above head
        double barWidth = width;
        double barHeight = 6.0;
        double barX = x;
        double barY = y - barHeight - 4.0;

        // Health bar background
        gc.setFill(Color.RED);
        gc.fillRect(barX, barY, barWidth, barHeight);

        // Health bar foreground (current HP)
        if (maxHp > 0 && hp > 0) {
            double hpRatio = Math.max(0.0, Math.min(1.0, (double) hp / maxHp));
            gc.setFill(Color.LIMEGREEN);
            gc.fillRect(barX, barY, barWidth * hpRatio, barHeight);
        }

        // Health bar border
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1.0);
        gc.strokeRect(barX, barY, barWidth, barHeight);
    }

    /**
     * Queries the terrain type under this zombie's center (or left edge) via {@link TerrainQuery}.
     */
    public TileType getCurrentTileType() {
        TerrainQuery query = terrainQuery != null ? terrainQuery : board;
        if (query == null) {
            return null;
        }
        TileType tileType = query.getTileType(lane, x + width / 2.0);
        if (tileType == null) {
            tileType = query.getTileType(lane, x);
        }
        return tileType;
    }

    /**
     * Checks whether this zombie's feet/center are on a WATER tile.
     */
    public boolean isOnWater() {
        return getCurrentTileType() == TileType.WATER;
    }

    // Getters and Setters
    public int getLane() {
        return lane;
    }

    public void setLane(int lane) {
        this.lane = lane;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public int getAttackDamage() {
        return attackDamage;
    }

    public void setAttackDamage(int attackDamage) {
        this.attackDamage = attackDamage;
    }

    public ZombieState getCurrentState() {
        return currentState;
    }

    public Entity getCurrentTarget() {
        return currentTarget;
    }

    public void setCurrentTarget(Entity currentTarget) {
        this.currentTarget = currentTarget;
    }

    public boolean isReadyToRemove() {
        return readyToRemove;
    }

    public void setReadyToRemove(boolean readyToRemove) {
        this.readyToRemove = readyToRemove;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public GameBoard getBoard() {
        return board;
    }

    public void setBoard(GameBoard board) {
        this.board = board;
        if (board != null) {
            this.terrainQuery = board;
        }
    }

    public TerrainQuery getTerrainQuery() {
        return terrainQuery != null ? terrainQuery : board;
    }

    public void setTerrainQuery(TerrainQuery terrainQuery) {
        this.terrainQuery = terrainQuery;
    }

    public LayeredTarget getCurrentCellTarget() {
        return currentCellTarget;
    }

    public void setCurrentCellTarget(LayeredTarget currentCellTarget) {
        this.currentCellTarget = currentCellTarget;
    }

    public boolean canSwim() {
        return canSwim || isWaterZombie();
    }

    public void setCanSwim(boolean canSwim) {
        this.canSwim = canSwim;
    }

    public boolean isDrowned() {
        return drowned;
    }

    public void setDrowned(boolean drowned) {
        this.drowned = drowned;
    }

    public Color getRenderColor() {
        return renderColor;
    }

    public void setRenderColor(Color renderColor) {
        this.renderColor = renderColor;
    }

    public double getAttackInterval() {
        return attackInterval;
    }

    public void setAttackInterval(double attackInterval) {
        this.attackInterval = attackInterval;
    }

    public double getSwimSpeed() {
        return swimSpeed;
    }

    public void setSwimSpeed(double swimSpeed) {
        this.swimSpeed = swimSpeed;
    }

    public boolean hasVaulted() {
        return hasVaulted;
    }

    public void setVaulted(boolean hasVaulted) {
        this.hasVaulted = hasVaulted;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        if (type != null && !type.isBlank()) {
            this.type = type;
        }
    }

    public boolean isWaterZombie() {
        return waterZombie || getClass().getSimpleName().equals("WaterZombie");
    }

    public void setWaterZombie(boolean waterZombie) {
        this.waterZombie = waterZombie;
    }
}
