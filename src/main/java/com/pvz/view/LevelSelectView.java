package com.pvz.view;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.pvz.builder.GameMap;
import com.pvz.builder.MapMode;
import com.pvz.core.GameManager;
import com.pvz.io.LevelDataLoader;
import com.pvz.state.game.GameState;
import com.pvz.state.game.MenuState;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/**
 * Lists the campaign levels and the player's saved custom maps; choosing one starts it.
 * <p>
 * Keyboard: UP/DOWN to move, ENTER to play, ESC back to the menu. Clicking a row plays it.
 * The list is reloaded every time this screen is entered, so newly saved maps show up.
 */
public final class LevelSelectView implements GameState {
    static final double LIST_TOP = 150.0;
    static final double ROW_HEIGHT = 48.0;
    static final double ROW_WIDTH = 620.0;
    static final int VISIBLE_ROWS = 10;

    private final LevelDataLoader loader;
    private final List<Entry> entries = new ArrayList<>();
    private int selected;
    private int firstVisible;
    private String notice;

    /** One row: a map and the text describing it. */
    record Entry(GameMap map, String tag, String detail) {
    }

    public LevelSelectView(LevelDataLoader loader) {
        if (loader == null) {
            throw new IllegalArgumentException("Loader cannot be null");
        }
        this.loader = loader;
    }

    @Override
    public void enter() {
        entries.clear();
        notice = null;
        selected = 0;
        firstVisible = 0;

        try {
            for (GameMap level : loader.loadCampaignLevels()) {
                entries.add(new Entry(level, "Campaign", describe(level)));
            }
        } catch (IllegalStateException e) {
            notice = "Không nạp được màn Campaign: " + e.getMessage();
        }

        try {
            LevelDataLoader.CustomMaps custom = loader.loadCustomMaps();
            for (LevelDataLoader.SavedMap saved : custom.maps()) {
                entries.add(new Entry(saved.map(), "Tự tạo", describe(saved.map())));
            }
            if (!custom.unreadable().isEmpty() && notice == null) {
                notice = custom.unreadable().size() + " file map bị hỏng nên đã bỏ qua";
            }
        } catch (IOException e) {
            if (notice == null) {
                notice = "Không đọc được thư mục map tự tạo";
            }
        }
    }

    /** "5 wave · 50 Sun" for campaign, "Vô hạn · 150 Sun" or "5 turn · 150 Sun" for custom. */
    static String describe(GameMap map) {
        String length;
        if (map.getMode() == MapMode.CAMPAIGN) {
            length = map.getWaves().size() + " wave";
        } else {
            length = map.isEndless() ? "Vô hạn" : map.getMaxTurns() + " turn";
        }
        return length + " · " + map.getStartingSun() + " Sun";
    }

    @Override
    public void update(double deltaTime) {
    }

    @Override
    public void render(GraphicsContext gc) {
        double width = gc.getCanvas().getWidth();
        double height = gc.getCanvas().getHeight();
        double listX = (width - ROW_WIDTH) / 2.0;

        gc.setFill(Color.web("#1E293B"));
        gc.fillRect(0, 0, width, height);

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(Color.web("#4ADE80"));
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 40));
        gc.fillText("CHỌN MÀN", width / 2.0, 100);

        int last = Math.min(entries.size(), firstVisible + VISIBLE_ROWS);
        for (int i = firstVisible; i < last; i++) {
            renderRow(gc, entries.get(i), i == selected, listX, rowTop(i));
        }

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Arial", FontWeight.NORMAL, 16));
        if (entries.stream().noneMatch(e -> e.map().getMode() == MapMode.CUSTOM)) {
            gc.setFill(Color.web("#94A3B8"));
            gc.fillText("Chưa có map tự tạo nào", width / 2.0, rowTop(last) + 30);
        }
        if (notice != null) {
            gc.setFill(Color.web("#FBBF24"));
            gc.fillText(notice, width / 2.0, height - 90);
        }
        gc.setFill(Color.web("#94A3B8"));
        gc.fillText("[↑][↓] Chọn   [ENTER] Chơi   [ESC] Về menu", width / 2.0, height - 50);
    }

    private static void renderRow(GraphicsContext gc, Entry entry, boolean isSelected,
            double x, double y) {
        gc.setFill(isSelected ? Color.web("#10B981") : Color.web("#334155"));
        gc.fillRoundRect(x, y, ROW_WIDTH, ROW_HEIGHT - 8, 10, 10);

        boolean campaign = entry.map().getMode() == MapMode.CAMPAIGN;
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        gc.setFill(campaign ? Color.web("#FDE68A") : Color.web("#BAE6FD"));
        gc.fillText(entry.tag().toUpperCase(), x + 16, y + 25);

        gc.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        gc.setFill(Color.WHITE);
        String name = entry.map().getName().isBlank() ? "(chưa đặt tên)" : entry.map().getName();
        gc.fillText(name, x + 120, y + 26);

        gc.setTextAlign(TextAlignment.RIGHT);
        gc.setFont(Font.font("Arial", FontWeight.NORMAL, 15));
        gc.setFill(Color.web("#E2E8F0"));
        gc.fillText(entry.detail(), x + ROW_WIDTH - 16, y + 25);
    }

    private double rowTop(int index) {
        return LIST_TOP + (index - firstVisible) * ROW_HEIGHT;
    }

    @Override
    public void handleMouseClick(MouseEvent event) {
        int index = rowAt(event.getX(), event.getY(), GameCanvas.DEFAULT_WIDTH);
        if (index >= 0) {
            selected = index;
            playSelected();
        }
    }

    /** @return the entry index under the point, or -1 if none */
    int rowAt(double x, double y, double canvasWidth) {
        double listX = (canvasWidth - ROW_WIDTH) / 2.0;
        if (x < listX || x > listX + ROW_WIDTH || y < LIST_TOP) {
            return -1;
        }
        int row = (int) ((y - LIST_TOP) / ROW_HEIGHT);
        int index = firstVisible + row;
        if (row >= VISIBLE_ROWS || index >= entries.size()) {
            return -1;
        }
        return index;
    }

    @Override
    public void handleKeyPressed(KeyEvent event) {
        KeyCode code = event.getCode();
        if (code == KeyCode.UP) {
            select(selected - 1);
        } else if (code == KeyCode.DOWN) {
            select(selected + 1);
        } else if (code == KeyCode.ENTER || code == KeyCode.SPACE) {
            playSelected();
        } else if (code == KeyCode.ESCAPE) {
            GameManager.getInstance().changeState(new MenuState());
        }
    }

    /** Moves the selection, clamped to the list, and scrolls so it stays visible. */
    private void select(int index) {
        if (entries.isEmpty()) {
            return;
        }
        selected = Math.max(0, Math.min(entries.size() - 1, index));
        if (selected < firstVisible) {
            firstVisible = selected;
        } else if (selected >= firstVisible + VISIBLE_ROWS) {
            firstVisible = selected - VISIBLE_ROWS + 1;
        }
    }

    private void playSelected() {
        if (!entries.isEmpty()) {
            GameManager.getInstance().startNewGame(entries.get(selected).map());
        }
    }

    @Override
    public void exit() {
    }

    List<Entry> getEntries() {
        return List.copyOf(entries);
    }

    int getSelected() {
        return selected;
    }

    int getFirstVisible() {
        return firstVisible;
    }

    String getNotice() {
        return notice;
    }
}
