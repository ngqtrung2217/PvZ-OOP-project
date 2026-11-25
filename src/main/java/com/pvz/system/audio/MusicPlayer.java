package com.pvz.system.audio;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.net.URL;

/**
 * Singleton background-music (BGM) player using JavaFX {@link Media} and
 * {@link MediaPlayer} with indefinite looping.
 * <p>
 * Fault-tolerant: missing resources or an uninitialized JavaFX toolkit in headless
 * tests are handled gracefully without throwing exceptions.
 */
public final class MusicPlayer {
    public static final String BGM_MENU = "/assets/audio/bgm/menu.wav";
    public static final String BGM_DAY_THEME = "/assets/audio/bgm/day_theme.wav";

    private static final MusicPlayer INSTANCE = new MusicPlayer();

    private MediaPlayer mediaPlayer;
    private String currentTrack;
    private boolean muted = false;
    private double volume = 0.5;

    private MusicPlayer() {
    }

    public static MusicPlayer getInstance() {
        return INSTANCE;
    }

    /**
     * Loads and plays a looping BGM track from the classpath.
     * Stops any currently playing track before starting the new one.
     */
    public void play(String resourcePath) {
        String normalizedPath = normalizePath(resourcePath);
        if (normalizedPath == null) {
            return;
        }

        stop();

        URL url = getClass().getResource(normalizedPath);
        if (url == null) {
            return;
        }

        try {
            Media media = new Media(url.toExternalForm());
            MediaPlayer player = new MediaPlayer(media);
            player.setCycleCount(MediaPlayer.INDEFINITE);
            player.setVolume(volume);
            player.setMute(muted);
            player.play();
            this.mediaPlayer = player;
            this.currentTrack = normalizedPath;
        } catch (Exception | LinkageError ignored) {
            this.mediaPlayer = null;
            this.currentTrack = null;
        }
    }

    /**
     * Stops and disposes the active BGM player, if any.
     */
    public void stop() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
                mediaPlayer.dispose();
            } catch (Exception | LinkageError ignored) {
                // Safe cleanup
            } finally {
                mediaPlayer = null;
            }
        }
        currentTrack = null;
    }

    /**
     * Pauses the active BGM track, if any.
     */
    public void pause() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.pause();
            } catch (Exception | LinkageError ignored) {
                // Safe no-op
            }
        }
    }

    /**
     * Resumes the active BGM track, if any.
     */
    public void resume() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.play();
            } catch (Exception | LinkageError ignored) {
                // Safe no-op
            }
        }
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
        if (mediaPlayer != null) {
            try {
                mediaPlayer.setMute(muted);
            } catch (Exception | LinkageError ignored) {
                // Safe no-op
            }
        }
    }

    public boolean isMuted() {
        return muted;
    }

    public void setVolume(double volume) {
        if (Double.isNaN(volume)) {
            return;
        }
        this.volume = Math.max(0.0, Math.min(1.0, volume));
        if (mediaPlayer != null) {
            try {
                mediaPlayer.setVolume(this.volume);
            } catch (Exception | LinkageError ignored) {
                // Safe no-op
            }
        }
    }

    public double getVolume() {
        return volume;
    }

    public String getCurrentTrack() {
        return currentTrack;
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String trimmed = path.trim();
        return trimmed.startsWith("/") ? trimmed : "/" + trimmed;
    }
}
