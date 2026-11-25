package com.pvz.system;

import com.pvz.system.audio.MusicPlayer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Headless unit tests for {@link MusicPlayer}.
 */
class MusicPlayerTest {

    private MusicPlayer musicPlayer;

    @BeforeEach
    void setUp() {
        musicPlayer = MusicPlayer.getInstance();
        musicPlayer.stop();
        musicPlayer.setMuted(false);
        musicPlayer.setVolume(0.5);
    }

    @AfterEach
    void tearDown() {
        musicPlayer.stop();
    }

    @Test
    @DisplayName("Singleton returns the same MusicPlayer instance")
    void testSingletonInstance() {
        assertSame(musicPlayer, MusicPlayer.getInstance());
    }

    @Test
    @DisplayName("setMuted and setVolume clamp values between 0.0 and 1.0")
    void testMuteAndVolumeClamping() {
        musicPlayer.setMuted(true);
        assertTrue(musicPlayer.isMuted());
        musicPlayer.setMuted(false);
        assertFalse(musicPlayer.isMuted());

        musicPlayer.setVolume(2.0);
        assertEquals(1.0, musicPlayer.getVolume(), 1e-9);

        musicPlayer.setVolume(-1.0);
        assertEquals(0.0, musicPlayer.getVolume(), 1e-9);

        musicPlayer.setVolume(0.35);
        assertEquals(0.35, musicPlayer.getVolume(), 1e-9);
    }

    @Test
    @DisplayName("play, pause, resume, and stop are safe in headless mode for valid and invalid paths")
    void testHeadlessPlaybackLifecycle() {
        assertDoesNotThrow(() -> musicPlayer.play(null));
        assertNull(musicPlayer.getCurrentTrack());

        assertDoesNotThrow(() -> musicPlayer.play("   "));
        assertNull(musicPlayer.getCurrentTrack());

        assertDoesNotThrow(() -> musicPlayer.play("/assets/audio/bgm/does_not_exist.wav"));
        assertNull(musicPlayer.getCurrentTrack());

        assertDoesNotThrow(() -> musicPlayer.play(MusicPlayer.BGM_MENU));
        assertDoesNotThrow(() -> musicPlayer.pause());
        assertDoesNotThrow(() -> musicPlayer.resume());
        assertDoesNotThrow(() -> musicPlayer.stop());
        assertNull(musicPlayer.getCurrentTrack());
    }
}
