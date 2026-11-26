package com.pvz.io;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;

import com.pvz.builder.GameMap;
import com.pvz.model.board.GameBoard;

/**
 * Identifies a map layout for the record table (README section 3.8): SHA-256 of the
 * tiles and the allowed plants only. Name, starting sun and turn count are left out, so
 * playing the same layout with other settings shares one record table.
 */
public final class MapIdHasher {
    private MapIdHasher() {
    }

    /** @return 64 lowercase hex characters */
    public static String hash(GameMap map) {
        StringBuilder text = new StringBuilder();
        for (int r = 0; r < GameBoard.ROWS; r++) {
            for (int c = 0; c < GameBoard.COLS; c++) {
                text.append(map.getTileType(r, c).name()).append(',');
            }
            text.append(';');
        }
        text.append('|');

        // Sorted: reordering the seed bar does not change the layout
        List<String> plants = new ArrayList<>(map.getAllowedPlants());
        Collections.sort(plants);
        text.append(String.join(",", plants));

        return HexFormat.of().formatHex(sha256(text.toString()));
    }

    private static byte[] sha256(String text) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            // Every Java runtime must provide SHA-256
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
