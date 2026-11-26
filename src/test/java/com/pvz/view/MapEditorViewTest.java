package com.pvz.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.pvz.builder.CustomMapBuilder;
import com.pvz.builder.LevelDirector;
import com.pvz.core.GameManager;
import com.pvz.io.MapSerializer;
import com.pvz.io.UserDataPaths;
import com.pvz.model.board.GameBoard;
import com.pvz.model.tile.TileType;
import com.pvz.state.game.GameOverState;
import com.pvz.state.game.MenuState;
import com.pvz.state.game.PlayingState;
import com.pvz.state.game.RunResult;
import javafx.event.EventType;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

/**
 * Unit tests for MapEditorView, driven through synthetic mouse and key events without
 * rendering.
 */
class MapEditorViewTest {
    private static final List<String> PLANTS = LevelDirector.PLANTS_USED;

    @TempDir
    Path root;
    private MapEditorController controller;
    private MapEditorView view;
    private GameManager manager;

    @BeforeEach
    void setUp() {
        MapSerializer serializer = new MapSerializer(Set.copyOf(PLANTS));
        controller = new MapEditorController(PLANTS, Set.of("PEASHOOTER"), serializer,
                new UserDataPaths(root));
        view = new MapEditorView(controller);
        manager = GameManager.getInstance();
        manager.startNewGame(new CustomMapBuilder(Set.of()).build());
        manager.changeState(view);
    }

    private static MouseEvent mouse(EventType<MouseEvent> type, double x, double y) {
        return new MouseEvent(type, x, y, x, y, MouseButton.PRIMARY, 1, false, false, false,
                false, true, false, false, false, false, false, null);
    }

    private void press(double x, double y) {
        view.handleMousePressed(mouse(MouseEvent.MOUSE_PRESSED, x, y));
    }

    private static KeyEvent key(KeyCode code) {
        return new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, false, false, false, false);
    }

    private static KeyEvent typed(String text) {
        return new KeyEvent(KeyEvent.KEY_TYPED, text, "", KeyCode.UNDEFINED, false, false,
                false, false);
    }

    private static double cellX(int col) {
        return GameBoard.START_X + col * GameBoard.CELL_WIDTH + GameBoard.CELL_WIDTH / 2;
    }

    private static double cellY(int row) {
        return GameBoard.START_Y + row * GameBoard.CELL_HEIGHT + GameBoard.CELL_HEIGHT / 2;
    }

    @Test
    @DisplayName("Dragging across the board paints every cell passed over")
    void testDragPaint() {
        press(MapEditorView.PANEL_X + 10, 125 + 45 + 10); // "Nước" brush
        assertEquals(TileType.WATER, controller.getBrush());

        press(cellX(0), cellY(2));
        for (int c = 1; c < 4; c++) {
            view.handleMouseDragged(mouse(MouseEvent.MOUSE_DRAGGED, cellX(c), cellY(2)));
        }
        view.handleMouseReleased(mouse(MouseEvent.MOUSE_RELEASED, cellX(3), cellY(2)));
        view.handleMouseDragged(mouse(MouseEvent.MOUSE_DRAGGED, cellX(5), cellY(2)));

        for (int c = 0; c < 4; c++) {
            assertEquals(TileType.WATER, controller.getTile(2, c));
        }
        assertEquals(TileType.GRASS, controller.getTile(2, 5), "No painting after release");
    }

    @Test
    @DisplayName("Keys 1-3 pick the brush")
    void testBrushKeys() {
        view.handleKeyPressed(key(KeyCode.DIGIT3));
        assertEquals(TileType.CRATER, controller.getBrush());
        view.handleKeyPressed(key(KeyCode.DIGIT1));
        assertEquals(TileType.GRASS, controller.getBrush());
    }

    @Test
    @DisplayName("Clicking the name field and typing sets the name; ENTER leaves the field")
    void testNameField() {
        view.handleKeyTyped(typed("x"));
        assertEquals("", controller.getName(), "Typing outside the field does nothing");

        press(MapEditorView.NAME_X + 20, MapEditorView.NAME_Y + 20);
        assertTrue(view.isNameFocused());
        view.handleKeyTyped(typed("A"));
        view.handleKeyTyped(typed("o"));
        view.handleKeyPressed(key(KeyCode.BACK_SPACE));
        view.handleKeyTyped(typed("b"));
        assertEquals("Ab", controller.getName());

        // While typing, digit keys type text instead of changing the brush
        view.handleKeyPressed(key(KeyCode.DIGIT3));
        assertEquals(TileType.WATER, controller.getBrush());

        view.handleKeyPressed(key(KeyCode.ENTER));
        assertFalse(view.isNameFocused());
        assertSame(view, manager.getCurrentState(), "ENTER in the field does not leave");
    }

    @Test
    @DisplayName("Sun, turn and plant buttons change the controller")
    void testPanelButtons() {
        press(MapEditorView.PANEL_X + MapEditorView.PANEL_W - 20, 430); // sun +
        assertEquals(75, controller.getStartingSun());

        press(MapEditorView.PANEL_X + MapEditorView.PANEL_W - 20, 510); // turns +
        assertFalse(controller.isEndless());
        assertEquals(6, controller.getTurns());

        int lilyPad = PLANTS.indexOf("LILY_PAD");
        press(MapEditorView.PANEL_X + lilyPad * MapEditorView.PLANT_STEP + 10,
                MapEditorView.PLANT_Y + 10);
        assertTrue(controller.isPlantAllowed("LILY_PAD"));
    }

    @Test
    @DisplayName("Play-test starts a practice game; ESC comes back to the same editor")
    void testPlayTestReturnsToEditor() {
        controller.setName("Thu");
        controller.setBrush(TileType.CRATER);
        controller.paint(0, 0);

        press(MapEditorView.PLAY_X + 10, MapEditorView.ACTION_Y + 10);

        PlayingState playing = assertInstanceOf(PlayingState.class, manager.getCurrentState());
        assertTrue(playing.isPractice());
        assertEquals(TileType.CRATER, playing.getMap().getTileType(0, 0));

        playing.handleKeyPressed(key(KeyCode.ESCAPE));
        assertSame(view, manager.getCurrentState());
        assertEquals(TileType.CRATER, controller.getTile(0, 0), "Edits survive the play-test");
    }

    @Test
    @DisplayName("After a lost play-test, retry stays a practice game and ESC returns")
    void testPracticeGameOver() {
        manager.startPracticeGame(controller.buildMap(), view);
        RunResult result = new RunResult(controller.buildMap(), RunResult.Outcome.LOST, 5, 0, true);
        manager.changeState(new GameOverState(result, view));

        manager.getCurrentState().handleKeyPressed(key(KeyCode.ENTER));
        PlayingState retry = assertInstanceOf(PlayingState.class, manager.getCurrentState());
        assertTrue(retry.isPractice());

        manager.changeState(new GameOverState(result, view));
        manager.getCurrentState().handleKeyPressed(key(KeyCode.ESCAPE));
        assertSame(view, manager.getCurrentState());
    }

    @Test
    @DisplayName("Save button saves; Open dialog lists the map and opens it")
    void testSaveAndOpenDialog() {
        controller.setName("Ao nha");
        controller.changeSun(4);
        press(MapEditorView.SAVE_X + 10, MapEditorView.ACTION_Y + 10);
        assertEquals("Đã lưu: Ao_nha.json", controller.getMessage());

        controller.setName("Khac");
        controller.changeSun(-4);

        press(MapEditorView.OPEN_X + 10, MapEditorView.ACTION_Y + 10);
        assertTrue(view.isOpenDialogShown());

        // While the dialog is open, ESC closes it instead of leaving the editor
        view.handleKeyPressed(key(KeyCode.ESCAPE));
        assertFalse(view.isOpenDialogShown());
        assertSame(view, manager.getCurrentState());

        press(MapEditorView.OPEN_X + 10, MapEditorView.ACTION_Y + 10);
        press(MapEditorView.DIALOG_X + 50, MapEditorView.DIALOG_Y + 60 + 10);

        assertFalse(view.isOpenDialogShown());
        assertEquals("Ao nha", controller.getName());
        assertEquals(150, controller.getStartingSun());
    }

    @Test
    @DisplayName("Clicking outside the open dialog closes it without opening anything")
    void testOpenDialogDismiss() {
        press(MapEditorView.OPEN_X + 10, MapEditorView.ACTION_Y + 10);
        press(5, 5);
        assertFalse(view.isOpenDialogShown());
        assertEquals("", controller.getName());
    }

    @Test
    @DisplayName("ESC outside the name field goes back to the menu")
    void testEscapeToMenu() {
        view.handleKeyPressed(key(KeyCode.ESCAPE));
        assertInstanceOf(MenuState.class, manager.getCurrentState());
    }

    @Test
    @DisplayName("Plant IDs become readable labels")
    void testDisplayName() {
        assertEquals("Snow Pea", MapEditorView.displayName("SNOW_PEA"));
        assertEquals("Peashooter", MapEditorView.displayName("PEASHOOTER"));
        assertEquals("Lily Pad", MapEditorView.displayName("LILY_PAD"));
    }
}
