package com.pvz.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for UserDataPaths: locations and map file names.
 */
class UserDataPathsTest {
    @Test
    @DisplayName("Real data lives in ~/.pvz")
    void testUserHome() {
        UserDataPaths paths = UserDataPaths.inUserHome();
        assertEquals(Path.of(System.getProperty("user.home"), ".pvz"), paths.getRoot());
        assertEquals(paths.getRoot().resolve("maps"), paths.getMapsDir());
        assertEquals(paths.getRoot().resolve("records.json"), paths.getRecordsFile());
    }

    @Test
    @DisplayName("Map names become safe file names, keeping Vietnamese letters")
    void testFileNames() {
        assertEquals("Ao_giữa_sân", UserDataPaths.toFileName("  Ao: giữa/sân? "));
        assertEquals("map", UserDataPaths.toFileName("..map"));
        assertEquals("_CON", UserDataPaths.toFileName("con".toUpperCase()));
        assertEquals(100, UserDataPaths.toFileName("a".repeat(150)).length());
    }

    @Test
    @DisplayName("Names with nothing usable are rejected")
    void testUnusableNames() {
        assertThrows(IllegalArgumentException.class, () -> UserDataPaths.toFileName("  "));
        assertThrows(IllegalArgumentException.class, () -> UserDataPaths.toFileName("???"));
        assertThrows(IllegalArgumentException.class, () -> UserDataPaths.toFileName(null));
    }

    @Test
    @DisplayName("mapFile puts the map in the maps folder with .json")
    void testMapFile(@TempDir Path root) {
        Path file = new UserDataPaths(root).mapFile("Bể bơi");
        assertEquals(root.resolve("maps").resolve("Bể_bơi.json"), file);
    }

    @Test
    @DisplayName("Listing returns only .json files, sorted, and nothing before the first save")
    void testListMapFiles(@TempDir Path root) throws Exception {
        UserDataPaths paths = new UserDataPaths(root);
        assertTrue(paths.listMapFiles().isEmpty());

        Files.createDirectories(paths.getMapsDir());
        Files.writeString(paths.getMapsDir().resolve("b.json"), "{}");
        Files.writeString(paths.getMapsDir().resolve("a.json"), "{}");
        Files.writeString(paths.getMapsDir().resolve("notes.txt"), "x");
        Files.createDirectories(paths.getMapsDir().resolve("folder.json"));

        assertEquals(List.of(paths.getMapsDir().resolve("a.json"),
                paths.getMapsDir().resolve("b.json")), paths.listMapFiles());
    }
}
