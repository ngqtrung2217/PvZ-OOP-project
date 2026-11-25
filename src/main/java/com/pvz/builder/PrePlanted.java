package com.pvz.builder;

/**
 * A plant already standing on the board when the map starts.
 * <p>
 * Immutable, and stores an ID instead of a plant object so a map stays plain data.
 * Board bounds and whether the ID exists are checked by the map builder.
 */
public final class PrePlanted {
    private final int row; // row: 0-4
    private final int col; // col: 0-8
    private final String plantId;

    public PrePlanted(int row, int col, String plantId) {
        if (plantId == null || plantId.isBlank()) {
            throw new IllegalArgumentException("Plant ID cannot be blank");
        }
        this.row = row;
        this.col = col;
        this.plantId = plantId;
    }

    // Getters
    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public String getPlantId() {
        return plantId;
    }
}
