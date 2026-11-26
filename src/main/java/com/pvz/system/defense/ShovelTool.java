package com.pvz.system.defense;

import com.pvz.model.board.GameBoard;

import java.util.function.BiPredicate;

/**
 * Shovel tool allowing the player to uproot an existing plant from a grid cell.
 * Decoupled from concrete {@code Plant} classes via a {@link BiPredicate} callback.
 */
public class ShovelTool {
    private boolean active;

    public ShovelTool() {
        this.active = false;
    }

    public boolean isActive() {
        return active;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public void toggle() {
        this.active = !this.active;
    }

    /**
     * Attempts to use the shovel at the specified grid coordinates {@code (row, col)}.
     * Automatically deactivates the shovel if removal succeeds.
     *
     * @param board             The active {@link GameBoard} used for coordinate validation.
     * @param row               Grid row index (0 to {@code GameBoard.ROWS - 1}).
     * @param col               Grid column index (0 to {@code GameBoard.COLS - 1}).
     * @param removePlantAction Callback returning {@code true} if a plant was removed at {@code (row, col)}.
     * @return {@code true} if the shovel was active, coordinates were valid, and removal succeeded.
     */
    public boolean useAt(GameBoard board, int row, int col, BiPredicate<Integer, Integer> removePlantAction) {
        if (!active || board == null || removePlantAction == null || !board.isValidCoordinate(row, col)) {
            return false;
        }
        boolean removed = removePlantAction.test(row, col);
        if (removed) {
            deactivate();
        }
        return removed;
    }

    /**
     * Converts canvas pixel coordinates {@code (mouseX, mouseY)} to grid coordinates via
     * {@link GameBoard} and delegates to {@link #useAt(GameBoard, int, int, BiPredicate)}.
     */
    public boolean useAtPixel(GameBoard board, double mouseX, double mouseY,
                              BiPredicate<Integer, Integer> removePlantAction) {
        if (!active || board == null || removePlantAction == null) {
            return false;
        }
        int row = board.getRowAt(mouseY);
        int col = board.getColAt(mouseX);
        return useAt(board, row, col, removePlantAction);
    }
}
