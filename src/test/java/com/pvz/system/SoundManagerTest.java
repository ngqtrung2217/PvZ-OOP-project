package com.pvz.system;

import com.pvz.system.audio.MusicPlayer;
import com.pvz.system.audio.SoundManager;
import com.pvz.system.event.BombExplosionEvent;
import com.pvz.system.event.BulletHitEvent;
import com.pvz.system.event.CombatEventBus;
import com.pvz.system.event.PlantEatenEvent;
import com.pvz.system.event.SunCollectedEvent;
import com.pvz.system.resource.ResourceManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Headless unit tests for {@link SoundManager} and the bundled WAV audio assets.
 */
class SoundManagerTest {

    private SoundManager soundManager;
    private CombatEventBus eventBus;

    @BeforeEach
    void setUp() {
        soundManager = SoundManager.getInstance();
        soundManager.setMuted(false);
        soundManager.setVolume(0.7);
        eventBus = CombatEventBus.getInstance();
        eventBus.clear();
        ResourceManager.getInstance().clearCache();
    }

    @AfterEach
    void tearDown() {
        soundManager.unregisterListeners(eventBus);
        eventBus.clear();
        ResourceManager.getInstance().clearCache();
    }

    @Test
    @DisplayName("Singleton returns the same SoundManager instance")
    void testSingletonInstance() {
        assertSame(soundManager, SoundManager.getInstance());
    }

    @Test
    @DisplayName("All 7 bundled WAV audio assets exist on classpath with valid PCM WAV size > 44 bytes")
    void testAllAudioAssetsExistAndAreValidWav() throws IOException {
        List<String> expectedAssets = List.of(
                SoundManager.SFX_SHOOT,
                SoundManager.SFX_SPLAT,
                SoundManager.SFX_CHOMP,
                SoundManager.SFX_BOOM,
                SoundManager.SFX_SUN,
                MusicPlayer.BGM_MENU,
                MusicPlayer.BGM_DAY_THEME
        );

        for (String path : expectedAssets) {
            URL url = getClass().getResource(path);
            assertNotNull(url, "Audio asset should exist on classpath: " + path);
            try (InputStream is = url.openStream()) {
                byte[] bytes = is.readAllBytes();
                assertTrue(bytes.length > 44,
                        "WAV file must be larger than 44-byte header: " + path + " (actual=" + bytes.length + ")");
                // Verify RIFF....WAVE signature
                assertEquals('R', (char) bytes[0]);
                assertEquals('I', (char) bytes[1]);
                assertEquals('F', (char) bytes[2]);
                assertEquals('F', (char) bytes[3]);
                assertEquals('W', (char) bytes[8]);
                assertEquals('A', (char) bytes[9]);
                assertEquals('V', (char) bytes[10]);
                assertEquals('E', (char) bytes[11]);
            }
        }
    }

    @Test
    @DisplayName("setMuted and setVolume clamp values between 0.0 and 1.0")
    void testMuteAndVolumeClamping() {
        soundManager.setMuted(true);
        assertTrue(soundManager.isMuted());
        soundManager.setMuted(false);
        assertFalse(soundManager.isMuted());

        soundManager.setVolume(1.5);
        assertEquals(1.0, soundManager.getVolume(), 1e-9);

        soundManager.setVolume(-0.4);
        assertEquals(0.0, soundManager.getVolume(), 1e-9);

        soundManager.setVolume(0.45);
        assertEquals(0.45, soundManager.getVolume(), 1e-9);

        soundManager.setVolume(Double.NaN);
        assertEquals(0.45, soundManager.getVolume(), 1e-9, "NaN should not overwrite volume");
    }

    @Test
    @DisplayName("Preload, direct SFX calls, and CombatEventBus triggers do not throw in headless CLI")
    void testHeadlessPlaybackAndEventBusIntegration() {
        assertDoesNotThrow(() -> soundManager.preloadDefaults());
        assertDoesNotThrow(() -> soundManager.playShoot());
        assertDoesNotThrow(() -> soundManager.playSplat());
        assertDoesNotThrow(() -> soundManager.playChomp());
        assertDoesNotThrow(() -> soundManager.playBoom());
        assertDoesNotThrow(() -> soundManager.playSunCollected());
        assertDoesNotThrow(() -> soundManager.playSfx("/assets/audio/sfx/missing.wav"));
        assertDoesNotThrow(() -> soundManager.playSfx(null));

        soundManager.registerListeners(eventBus);
        assertEquals(1, eventBus.getListenerCount(BulletHitEvent.class));
        assertEquals(1, eventBus.getListenerCount(BombExplosionEvent.class));
        assertEquals(1, eventBus.getListenerCount(PlantEatenEvent.class));
        assertEquals(1, eventBus.getListenerCount(SunCollectedEvent.class));

        assertDoesNotThrow(() -> eventBus.publish(new BulletHitEvent(null, null, 20, false, 100.0, 200.0)));
        assertDoesNotThrow(() -> eventBus.publish(new BombExplosionEvent(300.0, 200.0, 120.0, 1800)));
        assertDoesNotThrow(() -> eventBus.publish(new PlantEatenEvent(1, 3)));
        assertDoesNotThrow(() -> eventBus.publish(new SunCollectedEvent(25, 150.0, 180.0)));

        soundManager.unregisterListeners(eventBus);
        assertEquals(0, eventBus.getListenerCount(BulletHitEvent.class));
        assertEquals(0, eventBus.getListenerCount(BombExplosionEvent.class));
        assertEquals(0, eventBus.getListenerCount(PlantEatenEvent.class));
        assertEquals(0, eventBus.getListenerCount(SunCollectedEvent.class));
    }
}
