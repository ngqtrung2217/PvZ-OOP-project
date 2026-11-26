package com.pvz.view;

import com.pvz.builder.GameMap;
import com.pvz.builder.MapMode;
import com.pvz.state.game.PlayingState;
import com.pvz.state.game.RunResult;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/**
 * Draws the end-of-game screen for a {@link RunResult}: outcome, map and stats.
 * The victory and game-over states own the input; this class only paints.
 * <p>
 * TODO(Quang): add "Kỷ lục mới! Hạng n" and the top 10 once RecordRepository exists.
 */
public final class ResultView {
    private static final Color WIN_BACKGROUND = Color.web("#064E3B");
    private static final Color LOSE_BACKGROUND = Color.web("#0F172A");

    /**
     * @param hints key hints shown at the bottom, one per line
     */
    public void render(GraphicsContext gc, RunResult result, String... hints) {
        double width = gc.getCanvas().getWidth();
        double height = gc.getCanvas().getHeight();
        boolean won = result.outcome() == RunResult.Outcome.WON;
        GameMap map = result.map();

        gc.setFill(won ? WIN_BACKGROUND : LOSE_BACKGROUND);
        gc.fillRect(0, 0, width, height);

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(won ? Color.web("#34D399") : Color.web("#EF4444"));
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 54));
        gc.fillText(won ? "CHIẾN THẮNG!" : "ZOMBIE ĐÃ VÀO NHÀ!", width / 2.0, height / 3.0);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        gc.fillText(map.getName(), width / 2.0, height / 3.0 + 50);

        // Stats panel
        double panelWidth = 420;
        double panelX = (width - panelWidth) / 2.0;
        double panelY = height / 3.0 + 80;
        gc.setFill(Color.rgb(255, 255, 255, 0.08));
        gc.fillRoundRect(panelX, panelY, panelWidth, 170, 14, 14);

        gc.setFont(Font.font("Arial", FontWeight.BOLD, 30));
        gc.setFill(Color.web("#FDE68A"));
        gc.fillText("Điểm: " + result.score(), width / 2.0, panelY + 44);

        gc.setFont(Font.font("Arial", FontWeight.NORMAL, 19));
        gc.setFill(Color.web("#E2E8F0"));
        gc.fillText("Thời gian sinh tồn: " + PlayingState.formatTime(result.survivalSeconds()),
                width / 2.0, panelY + 80);
        gc.fillText(progressLine(result), width / 2.0, panelY + 108);
        gc.fillText(killLine(result), width / 2.0, panelY + 136);
        gc.fillText("Xe cắt cỏ còn lại: " + result.mowersLeft(), width / 2.0, panelY + 160);

        if (result.practice()) {
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 18));
            gc.setFill(Color.web("#FBBF24"));
            gc.fillText("Chơi thử – không lưu kỷ lục", width / 2.0, panelY + 200);
        }

        gc.setFont(Font.font("Arial", FontWeight.NORMAL, 18));
        gc.setFill(Color.web("#94A3B8"));
        double hintY = height - 120;
        for (String hint : hints) {
            gc.fillText(hint, width / 2.0, hintY);
            hintY += 30;
        }
    }

    /** "Zombie hạ gục: 12" plus drownings when there were any. */
    static String killLine(RunResult result) {
        String line = "Zombie hạ gục: " + result.kills();
        return result.drowned() > 0 ? line + " · chết đuối: " + result.drowned() : line;
    }

    /** "Wave đã vượt qua: 3/5" for campaign; turn progress is not tracked yet for custom. */
    static String progressLine(RunResult result) {
        GameMap map = result.map();
        if (map.getMode() == MapMode.CAMPAIGN) {
            return "Wave đã vượt qua: " + result.wavesCleared() + "/" + map.getWaves().size();
        }
        return map.isEndless() ? "Chế độ: Vô hạn" : "Chế độ: " + map.getMaxTurns() + " turn";
    }
}
