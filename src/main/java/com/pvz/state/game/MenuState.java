package com.pvz.state.game;

import com.pvz.core.GameManager;
import com.pvz.io.LevelDataLoader;
import com.pvz.view.GameCanvas;
import com.pvz.view.LevelSelectView;
import com.pvz.view.MapEditorController;
import com.pvz.view.MapEditorView;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/**
 * Main menu state displayed on game launch.
 */
public class MenuState implements GameState {
    private static final double BUTTON_WIDTH = 260.0;
    private static final double BUTTON_HEIGHT = 55.0;
    private static final double BUTTON_TOP_OFFSET = 30.0;
    /** Distance from the top of START to the top of MAP EDITOR. */
    private static final double EDITOR_BUTTON_GAP = 75.0;

    /** Screen Y of the button's top edge; both render and hit-testing share it. */
    private static double buttonY(double canvasHeight) {
        return canvasHeight / 2.0 + BUTTON_TOP_OFFSET;
    }

    @Override
    public void enter() {
        // Prepare menu background music or assets
    }

    @Override
    public void update(double deltaTime) {
        // Any menu particle / background animation
    }

    @Override
    public void render(GraphicsContext gc) {
        double width = gc.getCanvas().getWidth();
        double height = gc.getCanvas().getHeight();

        // Dark slate background
        gc.setFill(Color.web("#1E293B"));
        gc.fillRect(0, 0, width, height);

        // Title
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(Color.web("#4ADE80"));
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 46));
        gc.fillText("PLANTS VS. ZOMBIES", width / 2.0, height / 3.0);

        gc.setFont(Font.font("Arial", FontWeight.NORMAL, 22));
        gc.setFill(Color.web("#F8FAFC"));
        gc.fillText("MAP BUILDER EDITION", width / 2.0, height / 3.0 + 40);

        // Start prompt button area
        double btnX = (width - BUTTON_WIDTH) / 2.0;
        double btnY = buttonY(height);
        gc.setFill(Color.web("#10B981"));
        gc.fillRoundRect(btnX, btnY, BUTTON_WIDTH, BUTTON_HEIGHT, 12, 12);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        gc.fillText("START GAME", width / 2.0, btnY + 35);

        // Map editor button, below START
        double editorY = btnY + EDITOR_BUTTON_GAP;
        gc.setFill(Color.web("#3B82F6"));
        gc.fillRoundRect(btnX, editorY, BUTTON_WIDTH, BUTTON_HEIGHT, 12, 12);
        gc.setFill(Color.WHITE);
        gc.fillText("MAP EDITOR", width / 2.0, editorY + 35);

        // Hint text
        gc.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        gc.setFill(Color.web("#94A3B8"));
        gc.fillText("[ENTER] Play   ·   [E] Map Editor", width / 2.0, height - 60);
    }

    @Override
    public void handleMouseClick(MouseEvent event) {
        double mx = event.getX();
        double my = event.getY();
        double canvasW = GameCanvas.DEFAULT_WIDTH;
        double canvasH = GameCanvas.DEFAULT_HEIGHT;

        double btnX = (canvasW - BUTTON_WIDTH) / 2.0;
        double btnY = buttonY(canvasH);
        boolean insideX = mx >= btnX && mx <= btnX + BUTTON_WIDTH;

        if (insideX && my >= btnY && my <= btnY + BUTTON_HEIGHT) {
            openLevelSelect();
        } else if (insideX && my >= btnY + EDITOR_BUTTON_GAP
                && my <= btnY + EDITOR_BUTTON_GAP + BUTTON_HEIGHT) {
            openMapEditor();
        }
    }

    @Override
    public void handleKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
            openLevelSelect();
        } else if (event.getCode() == KeyCode.E) {
            openMapEditor();
        }
    }

    private static void openMapEditor() {
        GameManager.getInstance().changeState(
                new MapEditorView(MapEditorController.createDefault()));
    }

    /** START opens the level-select screen. */
    private static void openLevelSelect() {
        GameManager.getInstance().changeState(new LevelSelectView(LevelDataLoader.createDefault()));
    }

    @Override
    public void exit() {
        // Clean up menu resources
    }
}
