package com.pvz.view;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import com.pvz.builder.MapIssue;
import com.pvz.builder.ValidationResult;
import com.pvz.core.GameManager;
import com.pvz.io.LevelDataLoader;
import com.pvz.model.board.GameBoard;
import com.pvz.model.tile.TileType;
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
 * The map editor screen: draws {@link MapEditorController}'s state on the canvas and turns
 * mouse and keyboard input into controller calls.
 * <p>
 * Layout: name field on top, tools on the left, the 5x9 board in its usual place, plant
 * checkboxes below it, then stats, messages and the Open / Save / Play-test buttons.
 * Terrain is painted by clicking or dragging over the board.
 * <p>
 * {@link #enter()} keeps everything, so coming back from a play-test finds the map as it
 * was left.
 */
public final class MapEditorView implements GameState {
    // Layout, shared by drawing and hit-testing
    static final double NAME_X = 220;
    static final double NAME_Y = 28;
    static final double NAME_W = 480;
    static final double NAME_H = 44;
    static final double PANEL_X = 20;
    static final double PANEL_W = 180;
    static final double BUTTON_H = 38;
    static final double PLANT_Y = 615;
    static final double PLANT_W = 155;
    static final double PLANT_STEP = 164;
    static final double ACTION_Y = 690;
    static final double ACTION_W = 88;
    static final double ACTION_H = 44;
    static final double OPEN_X = 724;
    static final double SAVE_X = 818;
    static final double PLAY_X = 912;
    static final double DIALOG_X = 262;
    static final double DIALOG_Y = 150;
    static final double DIALOG_W = 500;
    static final double DIALOG_ROW_H = 40;
    static final int DIALOG_ROWS = 10;

    private final MapEditorController controller;
    private final List<Button> buttons = new ArrayList<>();
    private boolean nameFocused;
    private boolean painting;
    /** Saved maps shown by the "Mở" dialog; null while it is closed. */
    private List<LevelDataLoader.SavedMap> openList;
    private double caretTime;

    /** A clickable box. {@code active} decides whether it is drawn highlighted. */
    private record Button(String label, double x, double y, double w, double h,
            Runnable action, BooleanSupplier active) {
        boolean contains(double px, double py) {
            return px >= x && px <= x + w && py >= y && py <= y + h;
        }
    }

    public MapEditorView(MapEditorController controller) {
        if (controller == null) {
            throw new IllegalArgumentException("Controller cannot be null");
        }
        this.controller = controller;
        createButtons();
    }

    private void createButtons() {
        double y = 125;
        addBrush("Cỏ", TileType.GRASS, y);
        addBrush("Nước", TileType.WATER, y + 45);
        addBrush("Hố", TileType.CRATER, y + 90);

        add("Sân cỏ", PANEL_X, 295, PANEL_W, BUTTON_H, controller::applyLawnTemplate);
        add("Bể bơi", PANEL_X, 340, PANEL_W, BUTTON_H, controller::applyPoolTemplate);

        add("−", PANEL_X, 420, 40, BUTTON_H, () -> controller.changeSun(-1));
        add("+", PANEL_X + PANEL_W - 40, 420, 40, BUTTON_H, () -> controller.changeSun(1));

        add("−", PANEL_X, 500, 40, BUTTON_H, () -> controller.changeTurns(-1));
        add("+", PANEL_X + PANEL_W - 40, 500, 40, BUTTON_H, () -> controller.changeTurns(1));
        buttons.add(new Button("Vô hạn", PANEL_X, 545, PANEL_W, BUTTON_H,
                controller::toggleEndless, controller::isEndless));

        List<String> plants = controller.getPlantIds();
        for (int i = 0; i < plants.size(); i++) {
            String id = plants.get(i);
            buttons.add(new Button(displayName(id), PANEL_X + i * PLANT_STEP, PLANT_Y,
                    PLANT_W, 36, () -> controller.togglePlant(id),
                    () -> controller.isPlantAllowed(id)));
        }

        add("Mở", OPEN_X, ACTION_Y, ACTION_W, ACTION_H, this::showOpenDialog);
        add("Lưu", SAVE_X, ACTION_Y, ACTION_W, ACTION_H, controller::save);
        add("Chơi thử", PLAY_X, ACTION_Y, ACTION_W, ACTION_H, this::playTest);
    }

    private void addBrush(String label, TileType type, double y) {
        buttons.add(new Button(label, PANEL_X, y, PANEL_W, BUTTON_H,
                () -> controller.setBrush(type), () -> controller.getBrush() == type));
    }

    private void add(String label, double x, double y, double w, double h, Runnable action) {
        buttons.add(new Button(label, x, y, w, h, action, () -> false));
    }

    /** "SNOW_PEA" → "Snow Pea", so new plants need no label table. */
    static String displayName(String plantId) {
        StringBuilder out = new StringBuilder();
        for (String word : plantId.toLowerCase().split("_")) {
            if (word.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.toString();
    }

    private void playTest() {
        nameFocused = false;
        GameManager.getInstance().startPracticeGame(controller.buildMap(), this);
    }

    private void showOpenDialog() {
        nameFocused = false;
        openList = controller.listSavedMaps();
    }

    // ---- Lifecycle ----

    @Override
    public void enter() {
        // Keeps every edit: this screen is re-entered after a play-test
        painting = false;
    }

    @Override
    public void update(double deltaTime) {
        caretTime += deltaTime;
    }

    @Override
    public void exit() {
        painting = false;
    }

    // ---- Input ----

    @Override
    public void handleMousePressed(MouseEvent event) {
        double x = event.getX();
        double y = event.getY();

        if (openList != null) {
            handleOpenDialogClick(x, y);
            return;
        }

        nameFocused = x >= NAME_X && x <= NAME_X + NAME_W && y >= NAME_Y && y <= NAME_Y + NAME_H;
        if (nameFocused) {
            return;
        }

        if (paintAt(x, y)) {
            painting = true;
            return;
        }
        for (Button button : buttons) {
            if (button.contains(x, y)) {
                button.action().run();
                return;
            }
        }
    }

    @Override
    public void handleMouseDragged(MouseEvent event) {
        if (painting) {
            paintAt(event.getX(), event.getY());
        }
    }

    @Override
    public void handleMouseReleased(MouseEvent event) {
        painting = false;
    }

    @Override
    public void handleMouseClick(MouseEvent event) {
        // Everything happens on press, so dragging can start painting at once
    }

    /** @return true if the point was on the board and a cell was painted */
    private boolean paintAt(double x, double y) {
        int col = colAt(x);
        int row = rowAt(y);
        if (row < 0 || col < 0) {
            return false;
        }
        controller.paint(row, col);
        return true;
    }

    private static int colAt(double x) {
        if (x < GameBoard.START_X) {
            return -1;
        }
        int col = (int) ((x - GameBoard.START_X) / GameBoard.CELL_WIDTH);
        return col < GameBoard.COLS ? col : -1;
    }

    private static int rowAt(double y) {
        if (y < GameBoard.START_Y) {
            return -1;
        }
        int row = (int) ((y - GameBoard.START_Y) / GameBoard.CELL_HEIGHT);
        return row < GameBoard.ROWS ? row : -1;
    }

    private void handleOpenDialogClick(double x, double y) {
        double listTop = DIALOG_Y + 60;
        boolean insideList = x >= DIALOG_X && x <= DIALOG_X + DIALOG_W && y >= listTop;
        int index = insideList ? (int) ((y - listTop) / DIALOG_ROW_H) : -1;
        if (index >= 0 && index < Math.min(DIALOG_ROWS, openList.size())) {
            controller.open(openList.get(index).map());
        }
        openList = null;
    }

    @Override
    public void handleKeyPressed(KeyEvent event) {
        KeyCode code = event.getCode();
        if (openList != null) {
            if (code == KeyCode.ESCAPE) {
                openList = null;
            }
            return;
        }
        if (nameFocused) {
            if (code == KeyCode.BACK_SPACE) {
                controller.deleteLastNameChar();
            } else if (code == KeyCode.ENTER || code == KeyCode.ESCAPE || code == KeyCode.TAB) {
                nameFocused = false;
            }
            return;
        }
        switch (code) {
            case DIGIT1 -> controller.setBrush(TileType.GRASS);
            case DIGIT2 -> controller.setBrush(TileType.WATER);
            case DIGIT3 -> controller.setBrush(TileType.CRATER);
            case ESCAPE -> GameManager.getInstance().changeState(new MenuState());
            default -> {
                // Other keys do nothing here
            }
        }
    }

    @Override
    public void handleKeyTyped(KeyEvent event) {
        if (nameFocused && openList == null) {
            controller.typeName(event.getCharacter());
        }
    }

    boolean isNameFocused() {
        return nameFocused;
    }

    boolean isOpenDialogShown() {
        return openList != null;
    }

    // ---- Drawing ----

    @Override
    public void render(GraphicsContext gc) {
        double width = gc.getCanvas().getWidth();
        double height = gc.getCanvas().getHeight();

        gc.setFill(Color.web("#1E293B"));
        gc.fillRect(0, 0, width, height);

        renderHeader(gc, width);
        renderPanelLabels(gc);
        renderBoard(gc);
        for (Button button : buttons) {
            renderButton(gc, button);
        }
        renderValues(gc);
        renderStatsAndMessages(gc);

        if (openList != null) {
            renderOpenDialog(gc, width, height);
        }
    }

    private void renderHeader(GraphicsContext gc, double width) {
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFill(Color.web("#4ADE80"));
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        gc.fillText("MAP EDITOR", PANEL_X, 58);

        gc.setFill(nameFocused ? Color.web("#F8FAFC") : Color.web("#CBD5E1"));
        gc.fillRoundRect(NAME_X, NAME_Y, NAME_W, NAME_H, 8, 8);
        gc.setStroke(nameFocused ? Color.web("#10B981") : Color.web("#64748B"));
        gc.setLineWidth(nameFocused ? 3 : 1);
        gc.strokeRoundRect(NAME_X, NAME_Y, NAME_W, NAME_H, 8, 8);

        gc.setFont(Font.font("Arial", FontWeight.NORMAL, 20));
        String name = controller.getName();
        boolean showCaret = nameFocused && (int) (caretTime * 2) % 2 == 0;
        if (name.isEmpty() && !nameFocused) {
            gc.setFill(Color.web("#64748B"));
            gc.fillText("Nhập tên map...", NAME_X + 14, NAME_Y + 29);
        } else {
            gc.setFill(Color.web("#0F172A"));
            gc.fillText(name + (showCaret ? "|" : ""), NAME_X + 14, NAME_Y + 29);
        }

        gc.setTextAlign(TextAlignment.RIGHT);
        gc.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        gc.setFill(Color.web("#94A3B8"));
        gc.fillText("[1][2][3] Đổi địa hình   [ESC] Menu", width - 20, 56);
    }

    private static void renderPanelLabels(GraphicsContext gc) {
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        gc.setFill(Color.web("#94A3B8"));
        gc.fillText("ĐỊA HÌNH (vẽ / kéo)", PANEL_X, 115);
        gc.fillText("MẪU NHANH", PANEL_X, 285);
        gc.fillText("SUN KHỞI ĐẦU", PANEL_X, 410);
        gc.fillText("SỐ TURN", PANEL_X, 490);
        gc.fillText("CÂY ĐƯỢC DÙNG", PANEL_X, PLANT_Y - 6);
    }

    private void renderBoard(GraphicsContext gc) {
        for (int r = 0; r < GameBoard.ROWS; r++) {
            for (int c = 0; c < GameBoard.COLS; c++) {
                double x = GameBoard.START_X + c * GameBoard.CELL_WIDTH;
                double y = GameBoard.START_Y + r * GameBoard.CELL_HEIGHT;
                renderCell(gc, controller.getTile(r, c), (r + c) % 2 == 0, x, y);
            }
        }
        gc.setStroke(Color.web("#0F172A"));
        gc.setLineWidth(2);
        gc.strokeRect(GameBoard.START_X, GameBoard.START_Y,
                GameBoard.COLS * GameBoard.CELL_WIDTH, GameBoard.ROWS * GameBoard.CELL_HEIGHT);
    }

    private static void renderCell(GraphicsContext gc, TileType type, boolean light,
            double x, double y) {
        double w = GameBoard.CELL_WIDTH;
        double h = GameBoard.CELL_HEIGHT;
        switch (type) {
            case GRASS -> {
                gc.setFill(light ? Color.web("#66BB6A") : Color.web("#4CAF50"));
                gc.fillRect(x, y, w, h);
            }
            case WATER -> {
                gc.setFill(light ? Color.web("#42A5F5") : Color.web("#2196F3"));
                gc.fillRect(x, y, w, h);
                gc.setStroke(Color.web("#BBDEFB"));
                gc.setLineWidth(1.5);
                gc.strokeLine(x + 14, y + 40, x + 34, y + 40);
                gc.strokeLine(x + 44, y + 62, x + 64, y + 62);
            }
            case CRATER -> {
                gc.setFill(light ? Color.web("#66BB6A") : Color.web("#4CAF50"));
                gc.fillRect(x, y, w, h);
                gc.setFill(Color.web("#5D4037"));
                gc.fillOval(x + 10, y + 22, w - 20, h - 44);
                gc.setFill(Color.web("#3E2723"));
                gc.fillOval(x + 22, y + 34, w - 44, h - 68);
            }
        }
    }

    private static void renderButton(GraphicsContext gc, Button button) {
        boolean active = button.active().getAsBoolean();
        gc.setFill(active ? Color.web("#10B981") : Color.web("#334155"));
        gc.fillRoundRect(button.x(), button.y(), button.w(), button.h(), 8, 8);
        gc.setStroke(active ? Color.web("#6EE7B7") : Color.web("#475569"));
        gc.setLineWidth(1);
        gc.strokeRoundRect(button.x(), button.y(), button.w(), button.h(), 8, 8);

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 15));
        gc.fillText(button.label(), button.x() + button.w() / 2, button.y() + button.h() / 2 + 5);
    }

    private void renderValues(GraphicsContext gc) {
        double centerX = PANEL_X + PANEL_W / 2;
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(Color.web("#FDE68A"));
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        gc.fillText(String.valueOf(controller.getStartingSun()), centerX, 446);

        gc.setFill(controller.isEndless() ? Color.web("#64748B") : Color.WHITE);
        gc.fillText(String.valueOf(controller.getTurns()), centerX, 526);
    }

    private void renderStatsAndMessages(GraphicsContext gc) {
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFont(Font.font("Arial", FontWeight.NORMAL, 15));
        gc.setFill(Color.web("#E2E8F0"));
        gc.fillText(String.format("Ô nước: %d   ·   Ô hố: %d   ·   Hệ số điểm: ×%.2f",
                controller.count(TileType.WATER), controller.count(TileType.CRATER),
                controller.getSunMultiplier()), PANEL_X, 678);

        List<String> lines = new ArrayList<>();
        List<Color> colors = new ArrayList<>();
        if (controller.getMessage() != null) {
            lines.add(controller.getMessage());
            colors.add(Color.web("#93C5FD"));
        }
        ValidationResult result = controller.validate();
        for (MapIssue issue : result.getErrors()) {
            lines.add("✖ " + issue.getMessage() + " (chặn lưu)");
            colors.add(Color.web("#F87171"));
        }
        for (MapIssue issue : result.getWarnings()) {
            lines.add("⚠ " + issue.getMessage());
            colors.add(Color.web("#FBBF24"));
        }

        int shown = Math.min(lines.size(), 3);
        for (int i = 0; i < shown; i++) {
            String text = lines.get(i);
            if (i == shown - 1 && lines.size() > shown) {
                text += "   (+" + (lines.size() - shown) + " nữa)";
            }
            gc.setFill(colors.get(i));
            gc.fillText(text, PANEL_X, 704 + i * 21);
        }
    }

    private void renderOpenDialog(GraphicsContext gc, double width, double height) {
        gc.setFill(Color.rgb(0, 0, 0, 0.6));
        gc.fillRect(0, 0, width, height);

        int rows = Math.min(DIALOG_ROWS, openList.size());
        double dialogH = 100 + Math.max(1, rows) * DIALOG_ROW_H;
        gc.setFill(Color.web("#1E293B"));
        gc.fillRoundRect(DIALOG_X - 20, DIALOG_Y, DIALOG_W + 40, dialogH, 14, 14);

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        gc.fillText("Mở map đã lưu", DIALOG_X + DIALOG_W / 2, DIALOG_Y + 38);

        double listTop = DIALOG_Y + 60;
        gc.setFont(Font.font("Arial", FontWeight.NORMAL, 17));
        if (openList.isEmpty()) {
            gc.setFill(Color.web("#94A3B8"));
            gc.fillText("Chưa có map nào được lưu", DIALOG_X + DIALOG_W / 2, listTop + 26);
        }
        for (int i = 0; i < rows; i++) {
            double y = listTop + i * DIALOG_ROW_H;
            gc.setFill(Color.web("#334155"));
            gc.fillRoundRect(DIALOG_X, y, DIALOG_W, DIALOG_ROW_H - 6, 8, 8);
            gc.setFill(Color.WHITE);
            String name = openList.get(i).map().getName();
            gc.fillText(name.isBlank() ? "(chưa đặt tên)" : name, DIALOG_X + DIALOG_W / 2, y + 23);
        }

        gc.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        gc.setFill(Color.web("#94A3B8"));
        gc.fillText("Click một map để mở · click ra ngoài hoặc [ESC] để đóng",
                DIALOG_X + DIALOG_W / 2, DIALOG_Y + dialogH - 14);
    }
}
