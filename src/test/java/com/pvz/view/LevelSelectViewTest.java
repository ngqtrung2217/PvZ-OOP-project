package com.pvz.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.pvz.builder.CustomMapBuilder;
import com.pvz.builder.GameMap;
import com.pvz.builder.LevelDirector;
import com.pvz.builder.MapMode;
import com.pvz.core.GameManager;
import com.pvz.io.LevelDataLoader;
import com.pvz.io.MapSerializer;
import com.pvz.io.UserDataPaths;
import com.pvz.state.game.MenuState;
import com.pvz.state.game.PlayingState;
import com.pvz.state.game.RunResult;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

/**
 * Unit tests for LevelSelectView, driven through its input handlers without rendering.
 */
class LevelSelectViewTest {
    private static final Set<String> PLANTS = Set.copyOf(LevelDirector.PLANTS_USED);

    @TempDir
    Path root;
    private UserDataPaths paths;
    private MapSerializer serializer;
    private GameManager manager;

    @BeforeEach
    void setUp() {
        paths = new UserDataPaths(root);
        serializer = new MapSerializer(PLANTS);
        manager = GameManager.getInstance();
        manager.startNewGame(new CustomMapBuilder(Set.of()).build());
    }

    private LevelSelectView open() {
        LevelSelectView view = new LevelSelectView(new LevelDataLoader(serializer, paths));
        manager.changeState(view);
        return view;
    }

    private void saveCustom(String name, int sun) throws Exception {
        GameMap map = new CustomMapBuilder(PLANTS).setName(name).setStartingSun(sun).build();
        serializer.save(map, paths.mapFile(name));
    }

    private static KeyEvent key(KeyCode code) {
        return new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, false, false, false, false);
    }

    private static MouseEvent click(double x, double y) {
        return new MouseEvent(MouseEvent.MOUSE_CLICKED, x, y, x, y, MouseButton.PRIMARY, 1,
                false, false, false, false, true, false, false, false, false, false, null);
    }

    @Test
    @DisplayName("Lists campaign levels first, then saved custom maps")
    void testListing() throws Exception {
        saveCustom("Ao nha", 150);

        LevelSelectView view = open();

        List<LevelSelectView.Entry> entries = view.getEntries();
        assertEquals(3, entries.size());
        assertEquals(MapMode.CAMPAIGN, entries.get(0).map().getMode());
        assertEquals(MapMode.CAMPAIGN, entries.get(1).map().getMode());
        assertEquals("Ao nha", entries.get(2).map().getName());
        assertEquals("Vô hạn · 150 Sun", entries.get(2).detail());
        assertEquals("5 wave · 50 Sun", entries.get(0).detail());
        assertNull(view.getNotice());
    }

    @Test
    @DisplayName("DOWN then ENTER starts the selected map with its sun")
    void testKeyboardPlay() throws Exception {
        saveCustom("Ao nha", 175);
        LevelSelectView view = open();

        view.handleKeyPressed(key(KeyCode.DOWN));
        view.handleKeyPressed(key(KeyCode.DOWN));
        view.handleKeyPressed(key(KeyCode.DOWN)); // already at the end, stays there
        assertEquals(2, view.getSelected());

        view.handleKeyPressed(key(KeyCode.ENTER));

        PlayingState playing = assertInstanceOf(PlayingState.class, manager.getCurrentState());
        assertEquals("Ao nha", playing.getMap().getName());
        assertEquals(175, manager.getSun());
    }

    @Test
    @DisplayName("UP at the top stays on the first row")
    void testSelectionClamped() {
        LevelSelectView view = open();
        view.handleKeyPressed(key(KeyCode.UP));
        assertEquals(0, view.getSelected());
    }

    @Test
    @DisplayName("Clicking a row plays that map; clicking elsewhere does nothing")
    void testMouseClick() {
        LevelSelectView view = open();
        double x = GameCanvas.DEFAULT_WIDTH / 2.0;

        view.handleMouseClick(click(5, LevelSelectView.LIST_TOP + 5));
        assertSame(view, manager.getCurrentState());

        view.handleMouseClick(click(x, LevelSelectView.LIST_TOP + LevelSelectView.ROW_HEIGHT + 5));
        PlayingState playing = assertInstanceOf(PlayingState.class, manager.getCurrentState());
        assertTrue(playing.getMap().getName().startsWith("Bể bơi"));
    }

    @Test
    @DisplayName("A broken map file is skipped and reported")
    void testBrokenFile() throws Exception {
        saveCustom("Tot", 100);
        Files.writeString(paths.getMapsDir().resolve("hong.json"), "{ not json");

        LevelSelectView view = open();

        assertEquals(3, view.getEntries().size());
        assertEquals("1 file map bị hỏng nên đã bỏ qua", view.getNotice());
    }

    @Test
    @DisplayName("Long lists scroll so the selection stays visible")
    void testScrolling() throws Exception {
        for (int i = 0; i < 12; i++) {
            saveCustom("Map " + (char) ('A' + i), 50);
        }
        LevelSelectView view = open();
        assertEquals(14, view.getEntries().size());

        for (int i = 0; i < 13; i++) {
            view.handleKeyPressed(key(KeyCode.DOWN));
        }

        assertEquals(13, view.getSelected());
        assertEquals(13 - LevelSelectView.VISIBLE_ROWS + 1, view.getFirstVisible());
    }

    @Test
    @DisplayName("Re-entering the screen picks up maps saved in the meantime")
    void testReloadOnEnter() throws Exception {
        LevelSelectView view = open();
        assertEquals(2, view.getEntries().size());

        saveCustom("Moi", 50);
        manager.changeState(new MenuState());
        manager.changeState(view);

        assertEquals(3, view.getEntries().size());
    }

    @Test
    @DisplayName("ESC goes back to the menu")
    void testEscape() {
        LevelSelectView view = open();
        view.handleKeyPressed(key(KeyCode.ESCAPE));
        assertInstanceOf(MenuState.class, manager.getCurrentState());
    }

    @Test
    @DisplayName("Result screen progress line depends on the map mode")
    void testResultProgressLine() {
        GameMap campaign = new LevelDirector().buildDayLevel(new CustomMapBuilder(PLANTS));
        GameMap endless = new CustomMapBuilder(PLANTS).build();
        GameMap limited = new CustomMapBuilder(PLANTS).setMaxTurns(4).build();

        assertEquals("Wave đã vượt qua: 3/5", ResultView.progressLine(
                new RunResult(campaign, RunResult.Outcome.LOST, 10, 3)));
        assertEquals("Chế độ: Vô hạn", ResultView.progressLine(
                new RunResult(endless, RunResult.Outcome.LOST, 10, 0)));
        assertEquals("Chế độ: 4 turn", ResultView.progressLine(
                new RunResult(limited, RunResult.Outcome.LOST, 10, 0)));
    }
}
