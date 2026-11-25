package com.pvz.system;

import com.pvz.model.board.GameBoard;
import com.pvz.system.defense.ShovelTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Headless JUnit 5 tests for {@link ShovelTool}.
 */
class ShovelToolTest {

    private ShovelTool shovel;
    private GameBoard board;

    @BeforeEach
    void setUp() {
        shovel = new ShovelTool();
        board = new GameBoard();
    }

    @Test
    @DisplayName("activate(), deactivate(), and toggle() update active state properly")
    void testActivateDeactivateToggle() {
        assertFalse(shovel.isActive());

        shovel.activate();
        assertTrue(shovel.isActive());

        shovel.deactivate();
        assertFalse(shovel.isActive());

        shovel.toggle();
        assertTrue(shovel.isActive());

        shovel.toggle();
        assertFalse(shovel.isActive());
    }

    @Test
    @DisplayName("useAt returns false when inactive, board is null, or coordinates are out of bounds")
    void testUseAtInvalidConditions() {
        AtomicInteger callbackCalls = new AtomicInteger(0);

        // Inactive shovel
        assertFalse(shovel.useAt(board, 2, 3, (r, c) -> {
            callbackCalls.incrementAndGet();
            return true;
        }));
        assertEquals(0, callbackCalls.get());

        // Active shovel with invalid board / callback / coordinates
        shovel.activate();
        assertFalse(shovel.useAt(null, 2, 3, (r, c) -> true));
        assertFalse(shovel.useAt(board, 2, 3, null));
        assertFalse(shovel.useAt(board, -1, 0, (r, c) -> {
            callbackCalls.incrementAndGet();
            return true;
        }));
        assertFalse(shovel.useAt(board, GameBoard.ROWS, 0, (r, c) -> {
            callbackCalls.incrementAndGet();
            return true;
        }));
        assertFalse(shovel.useAt(board, 0, -1, (r, c) -> {
            callbackCalls.incrementAndGet();
            return true;
        }));
        assertFalse(shovel.useAt(board, 0, GameBoard.COLS, (r, c) -> {
            callbackCalls.incrementAndGet();
            return true;
        }));

        assertEquals(0, callbackCalls.get());
        assertTrue(shovel.isActive(), "Failed shovel attempts should leave shovel active");
    }

    @Test
    @DisplayName("useAt invokes callback with exact (row, col) and auto-deactivates on success")
    void testUseAtValidCellDeactivatesOnSuccess() {
        AtomicInteger capturedRow = new AtomicInteger(-1);
        AtomicInteger capturedCol = new AtomicInteger(-1);

        shovel.activate();
        // Empty cell (callback returns false): shovel remains active
        assertFalse(shovel.useAt(board, 1, 2, (r, c) -> false));
        assertTrue(shovel.isActive());

        // Occupied cell (callback returns true): shovel deactivates
        boolean removed = shovel.useAt(board, 3, 6, (r, c) -> {
            capturedRow.set(r);
            capturedCol.set(c);
            return true;
        });

        assertTrue(removed);
        assertEquals(3, capturedRow.get());
        assertEquals(6, capturedCol.get());
        assertFalse(shovel.isActive(), "Shovel must automatically deactivate after successful removal");
    }

    @Test
    @DisplayName("useAtPixel maps canvas coordinates to (row, col) and auto-deactivates on success")
    void testUseAtPixelMapsCoordinatesAndDeactivates() {
        AtomicInteger capturedRow = new AtomicInteger(-1);
        AtomicInteger capturedCol = new AtomicInteger(-1);

        double pixelX = board.getCellCenterX(4);
        double pixelY = board.getCellCenterY(2);

        shovel.activate();
        // Outside grid pixels -> false
        assertFalse(shovel.useAtPixel(board, 10.0, 10.0, (r, c) -> true));
        assertTrue(shovel.isActive());

        // Inside cell (2, 4)
        boolean removed = shovel.useAtPixel(board, pixelX, pixelY, (r, c) -> {
            capturedRow.set(r);
            capturedCol.set(c);
            return true;
        });

        assertTrue(removed);
        assertEquals(2, capturedRow.get());
        assertEquals(4, capturedCol.get());
        assertFalse(shovel.isActive());
    }
}
