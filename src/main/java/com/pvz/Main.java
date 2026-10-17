package com.pvz;

import com.pvz.core.GameEngine;
import com.pvz.core.GameManager;
import com.pvz.core.InputManager;
import com.pvz.state.game.MenuState;
import com.pvz.view.GameCanvas;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Main application entry point for Plants vs. Zombies (Map Builder Edition).
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        GameCanvas canvas = new GameCanvas();
        StackPane root = new StackPane(canvas);
        Scene scene = new Scene(root, GameCanvas.DEFAULT_WIDTH, GameCanvas.DEFAULT_HEIGHT);

        // Core Engine setup
        GameEngine engine = new GameEngine(canvas);
        InputManager inputManager = new InputManager();
        inputManager.registerInputHandlers(canvas, scene);

        // Initialize starting game state
        GameManager.getInstance().changeState(new MenuState());

        // Start 60 FPS Game Loop
        engine.start();

        primaryStage.setTitle("Plants vs. Zombies - Map Builder");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.setOnCloseRequest(e -> engine.stop());
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
