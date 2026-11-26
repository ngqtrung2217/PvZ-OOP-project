package com.pvz.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Where player data lives: {@code ~/.pvz/maps/} and {@code ~/.pvz/records.json}.
 * <p>
 * Files inside the jar ({@code resources/}) are read-only, so anything the player saves
 * must go to the home folder. Tests pass a temporary folder as the root instead.
 */
public final class UserDataPaths {
    private static final String MAP_EXTENSION = ".json";
    private static final int MAX_FILE_NAME_LENGTH = 100;

    private final Path root;

    public UserDataPaths(Path root) {
        if (root == null) {
            throw new IllegalArgumentException("Root folder cannot be null");
        }
        this.root = root;
    }

    /** The real location, {@code ~/.pvz}. */
    public static UserDataPaths inUserHome() {
        return new UserDataPaths(Path.of(System.getProperty("user.home"), ".pvz"));
    }

    public Path getRoot() {
        return root;
    }

    public Path getMapsDir() {
        return root.resolve("maps");
    }

    public Path getRecordsFile() {
        return root.resolve("records.json");
    }

    /**
     * The file a custom map with this name is saved to. Saving again under the same name
     * overwrites it.
     *
     * @throws IllegalArgumentException if nothing usable is left of the name
     */
    public Path mapFile(String mapName) {
        return getMapsDir().resolve(toFileName(mapName) + MAP_EXTENSION);
    }

    /** Saved map files, sorted by file name. Empty if nothing has been saved yet. */
    public List<Path> listMapFiles() throws IOException {
        Path dir = getMapsDir();
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(MAP_EXTENSION))
                    .sorted()
                    .toList();
        }
    }

    /**
     * Keeps letters (including Vietnamese), digits and simple punctuation; characters
     * Windows forbids in file names become spaces, and runs of spaces become one '_'.
     */
    static String toFileName(String mapName) {
        if (mapName == null) {
            throw new IllegalArgumentException("Map name cannot be null");
        }
        String name = mapName.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", " ")
                .strip()
                .replaceAll("\\s+", "_")
                .replaceAll("^\\.+", ""); // a leading dot hides the file on Linux and macOS
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Map name has no usable characters: " + mapName);
        }
        if (name.length() > MAX_FILE_NAME_LENGTH) {
            name = name.substring(0, MAX_FILE_NAME_LENGTH);
        }
        // Windows refuses these names whatever the extension
        if (name.matches("(?i)(con|prn|aux|nul|com\\d|lpt\\d)")) {
            name = "_" + name;
        }
        return name;
    }
}
