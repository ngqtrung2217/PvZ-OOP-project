package com.pvz.io;

/**
 * A map file could be read but does not describe a valid map. Checked, so callers that
 * load player files must decide what to show instead of crashing.
 */
public class MapFormatException extends Exception {
    public MapFormatException(String message) {
        super(message);
    }

    public MapFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
