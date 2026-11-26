package com.pvz.builder;

import java.util.List;

import com.pvz.model.tile.TileType;

/**
 * Builds a {@link GameMap} step by step. Every setter returns the builder so calls can be
 * chained.
 * <p>
 * The builder rejects values that can never be valid (a cell outside the grid, negative
 * sun). It accepts a map that is merely incomplete, such as one without a name or plants,
 * because the editor has to play-test those; {@code MapValidator} decides whether it may be
 * saved.
 */
public interface MapBuilder {
    /** A blank name is allowed here; saving a map without a name is blocked elsewhere. */
    MapBuilder setName(String name);

    MapBuilder setTile(int row, int col, TileType type);

    MapBuilder setStartingSun(int sun);

    /** Plants offered in the seed bar, in this order. Duplicates are kept only once. */
    MapBuilder setAllowedPlants(List<String> plantIds);

    /** Custom maps only: number of turns to survive, or {@code null} for endless. */
    MapBuilder setMaxTurns(Integer turns);

    /**
     * Campaign maps only: adding a wave makes the map a campaign level. Waves are played
     * in the order they are added.
     */
    MapBuilder addWave(WaveConfig wave);

    GameMap build();
}
