package com.pvz.system;

import com.pvz.system.resource.ResourceManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Headless unit tests for {@link ResourceManager}.
 * Runs without requiring a JavaFX window or toolkit initialization.
 */
class ResourceManagerTest {

    private ResourceManager resourceManager;

    @BeforeEach
    void setUp() {
        resourceManager = ResourceManager.getInstance();
        resourceManager.clearCache();
    }

    @AfterEach
    void tearDown() {
        resourceManager.clearCache();
    }

    @Test
    @DisplayName("Singleton returns the exact same ResourceManager instance")
    void testSingletonInstance() {
        ResourceManager first = ResourceManager.getInstance();
        ResourceManager second = ResourceManager.getInstance();
        assertSame(first, second);
    }

    @Test
    @DisplayName("getImage returns null safely for null, blank, or missing resource paths")
    void testGetImageInvalidAndMissingPaths() {
        assertNull(resourceManager.getImage(null));
        assertNull(resourceManager.getImage(""));
        assertNull(resourceManager.getImage("   "));
        assertNull(resourceManager.getImage("/assets/sprites/does_not_exist.png"));
        assertNull(resourceManager.getImage("assets/sprites/also_missing.png"));
        assertEquals(0, resourceManager.getCachedImageCount());
    }

    @Test
    @DisplayName("getAudioClip returns null safely for null, blank, or missing resource paths")
    void testGetAudioClipInvalidAndMissingPaths() {
        assertNull(resourceManager.getAudioClip(null));
        assertNull(resourceManager.getAudioClip(""));
        assertNull(resourceManager.getAudioClip("   "));
        assertNull(resourceManager.getAudioClip("/assets/audio/sfx/does_not_exist.wav"));
        assertNull(resourceManager.getAudioClip("assets/audio/sfx/also_missing.wav"));
        assertEquals(0, resourceManager.getCachedAudioCount());
    }

    @Test
    @DisplayName("preloadImages and preloadAudio handle null and missing entries without throwing")
    void testPreloadWithMissingOrNullInputs() {
        assertDoesNotThrow(() -> resourceManager.preloadImages((String[]) null));
        assertDoesNotThrow(() -> resourceManager.preloadImages((List<String>) null));
        assertDoesNotThrow(() -> resourceManager.preloadImages(
                "/assets/sprites/missing1.png",
                null,
                "   "
        ));
        assertDoesNotThrow(() -> resourceManager.preloadAudio((String[]) null));
        assertDoesNotThrow(() -> resourceManager.preloadAudio((List<String>) null));
        assertDoesNotThrow(() -> resourceManager.preloadAudio(
                "/assets/audio/sfx/missing1.wav",
                null,
                ""
        ));
    }

    @Test
    @DisplayName("clearCache executes cleanly and leaves caches empty")
    void testClearCache() {
        resourceManager.getImage("/assets/sprites/missing.png");
        resourceManager.getAudioClip("/assets/audio/sfx/missing.wav");

        assertDoesNotThrow(() -> resourceManager.clearCache());
        assertEquals(0, resourceManager.getCachedImageCount());
        assertEquals(0, resourceManager.getCachedAudioCount());
    }
}
