package com.pvz.state.game;

import com.pvz.core.GameManager;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/**
 * State representing game pause. Maintains reference to the active PlayingState.
 */
public class PauseState implements GameState {
    private final PlayingState previousPlayingState;

    public PauseState(PlayingState playingState) {
        this.previousPlayingState = playingState;
    }

    @Override
    public void enter() {}

    @Override
    public void update(double deltaTime) {
        // No gameplay updates while paused
    }

    @Override
    public void render(GraphicsContext gc) {
        // Render the paused game in the background first
        if (previousPlayingState != null) {
            previousPlayingState.render(gc);
        }

        double width = gc.getCanvas().getWidth();
        double height = gc.getCanvas().getHeight();

        // Dark translucent overlay
        gc.setFill(Color.rgb(0, 0, 0, 0.65));
        gc.fillRect(0, 0, width, height);

        // Pause text
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(Color.web("#FBBF24"));
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 42));
        gc.fillText("GAME PAUSED", width / 2.0, height / 2.0 - 40);

        gc.setFont(Font.font("Arial", FontWeight.NORMAL, 20));
        gc.setFill(Color.WHITE);
        gc.fillText("Press [P] to Resume", width / 2.0, height / 2.0 + 20);
        gc.fillText("Press [ESC] to Return to Menu", width / 2.0, height / 2.0 + 60);
    }

    @Override
    public void handleMouseClick(MouseEvent event) {}

    @Override
    public void handleKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.P) {
            GameManager.getInstance().changeState(previousPlayingState);
        } else if (event.getCode() == KeyCode.ESCAPE) {
            GameManager.getInstance().changeState(new MenuState());
        }
    }

    @Override
    public void exit() {}
}
