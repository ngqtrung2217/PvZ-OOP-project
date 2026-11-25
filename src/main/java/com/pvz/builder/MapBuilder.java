package com.pvz.builder;

import com.pvz.model.tile.TileType;

public interface MapBuilder {
    MapBuilder setTile(int row, int col, TileType type);

    MapBuilder setStartingSun(int sun);

    MapBuilder addPrePlanted(int row, int col, String plantId);

    /** Waves are played in the order they are added. */
    MapBuilder addWave(WaveConfig wave);

    GameMap build();
}