package com.pvz.builder;

import com.pvz.model.tile.TileType;

public interface MapBuilder {
    MapBuilder setTile(int row, int col, TileType type);

    MapBuilder setStartingSun(int sun);

    MapBuilder addPrePlanted(int row, int col, String plantId);

    GameMap build();
    // TODO(Ngoc Minh): addWave(Wave wave) when feat/zombie-states is merged
}