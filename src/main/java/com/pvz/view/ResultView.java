package com.pvz.view;

import java.util.List;

import com.pvz.builder.GameMap;
import com.pvz.builder.MapMode;
import com.pvz.io.RecordRepository;
import com.pvz.io.RunRecord;
import com.pvz.state.game.PlayingState;
import com.pvz.state.game.RecordSubmission;
import com.pvz.state.game.RunResult;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/**
 * Draws the end-of-game screen: outcome and stats on the left, the layout's top 10 on the
 * right, with "Kỷ lục mới! Hạng n" when the game made the table. The victory and
 * game-over states own the input and the saving; this class only paints.
 */
public final class ResultView {
    private static final Color WIN_BACKGROUND = Color.web("#064E3B");
    private static final Color LOSE_BACKGROUND = Color.web("#0F172A");
    private static final Color PANEL = Color.rgb(255, 255, 255, 0.08);
    private static final double STATS_X = 60;
    private static final double TABLE_X = 520;
    private static final double PANEL_Y = 220;
    private static final double PANEL_W = 444;
    private static final double ROW_H = 30;

    /**
     * @param records rank and top table; null draws the stats only
     * @param hints   key hints shown at the bottom, one per line
     */
    public void render(GraphicsContext gc, RunResult result, RecordSubmission records,
            String... hints) {
        double width = gc.getCanvas().getWidth();
        double height = gc.getCanvas().getHeight();
        boolean won = result.outcome() == RunResult.Outcome.WON;

        gc.setFill(won ? WIN_BACKGROUND : LOSE_BACKGROUND);
        gc.fillRect(0, 0, width, height);

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(won ? Color.web("#34D399") : Color.web("#EF4444"));
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 50));
        gc.fillText(won ? "CHIẾN THẮNG!" : "ZOMBIE ĐÃ VÀO NHÀ!", width / 2.0, 120);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        gc.fillText(result.map().getName(), width / 2.0, 165);

        renderStats(gc, result, records);
        renderTable(gc, result, records);

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Arial", FontWeight.NORMAL, 18));
        gc.setFill(Color.web("#94A3B8"));
        double hintY = height - 80;
        for (String hint : hints) {
            gc.fillText(hint, width / 2.0, hintY);
            hintY += 28;
        }
    }

    private static void renderStats(GraphicsContext gc, RunResult result,
            RecordSubmission records) {
        double centerX = STATS_X + PANEL_W / 2;
        gc.setFill(PANEL);
        gc.fillRoundRect(STATS_X, PANEL_Y, PANEL_W, 250, 14, 14);

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        gc.setFill(Color.web("#FDE68A"));
        gc.fillText("Điểm: " + result.score(), centerX, PANEL_Y + 50);

        gc.setFont(Font.font("Arial", FontWeight.NORMAL, 19));
        gc.setFill(Color.web("#E2E8F0"));
        gc.fillText("Thời gian sinh tồn: " + PlayingState.formatTime(result.survivalSeconds()),
                centerX, PANEL_Y + 92);
        gc.fillText(progressLine(result), centerX, PANEL_Y + 122);
        gc.fillText(killLine(result), centerX, PANEL_Y + 152);
        gc.fillText("Xe cắt cỏ còn lại: " + result.mowersLeft(), centerX, PANEL_Y + 182);

        gc.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        String status = statusLine(result, records);
        if (status != null) {
            gc.setFill(records != null && records.isNewRecord()
                    ? Color.web("#FBBF24") : Color.web("#94A3B8"));
            gc.fillText(status, centerX, PANEL_Y + 228);
        }
    }

    private static void renderTable(GraphicsContext gc, RunResult result,
            RecordSubmission records) {
        List<RunRecord> top = records == null ? List.of() : records.top();
        gc.setFill(PANEL);
        gc.fillRoundRect(TABLE_X, PANEL_Y, PANEL_W, 60 + 10 * ROW_H, 14, 14);

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 17));
        gc.setFill(Color.web("#CBD5E1"));
        gc.fillText("TOP 10 BỐ CỤC NÀY", TABLE_X + PANEL_W / 2, PANEL_Y + 30);

        if (top.isEmpty()) {
            gc.setFont(Font.font("Arial", FontWeight.NORMAL, 16));
            gc.setFill(Color.web("#94A3B8"));
            gc.fillText("Chưa có kỷ lục nào", TABLE_X + PANEL_W / 2, PANEL_Y + 80);
            return;
        }

        gc.setFont(Font.font("Arial", FontWeight.NORMAL, 16));
        for (int i = 0; i < top.size(); i++) {
            double y = PANEL_Y + 62 + i * ROW_H;
            boolean mine = records.rank() == i + 1;
            if (mine) {
                gc.setFill(Color.rgb(251, 191, 36, 0.25));
                gc.fillRoundRect(TABLE_X + 10, y - 20, PANEL_W - 20, ROW_H - 2, 6, 6);
            }
            RunRecord record = top.get(i);
            gc.setFill(mine ? Color.web("#FDE68A") : Color.web("#E2E8F0"));
            gc.setTextAlign(TextAlignment.LEFT);
            gc.fillText((i + 1) + ".", TABLE_X + 20, y);
            gc.fillText(record.score() + " điểm", TABLE_X + 60, y);
            gc.fillText(PlayingState.formatTime(record.survivalSeconds()), TABLE_X + 190, y);
            gc.fillText(record.won() ? "Thắng" : "Thua", TABLE_X + 260, y);
            gc.setTextAlign(TextAlignment.RIGHT);
            gc.fillText(record.startingSun() + " Sun · " + turnsLabel(record),
                    TABLE_X + PANEL_W - 20, y);
        }
    }

    /** The line under the stats: new record, not ranked, practice, or a save error. */
    static String statusLine(RunResult result, RecordSubmission records) {
        if (result.practice()) {
            return "Chơi thử – không lưu kỷ lục";
        }
        if (records == null) {
            return null;
        }
        if (records.error() != null) {
            return records.error();
        }
        return records.isNewRecord()
                ? "Kỷ lục mới! Hạng " + records.rank()
                : "Chưa vào top " + RecordRepository.MAX_PER_LAYOUT;
    }

    /** "∞" for endless or campaign maps, otherwise "5 turn". */
    static String turnsLabel(RunRecord record) {
        return record.maxTurns() == null ? "∞" : record.maxTurns() + " turn";
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
