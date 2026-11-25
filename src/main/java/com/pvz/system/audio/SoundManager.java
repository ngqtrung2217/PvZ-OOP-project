package com.pvz.system.audio;

import com.pvz.system.event.BombExplosionEvent;
import com.pvz.system.event.BulletHitEvent;
import com.pvz.system.event.CombatEventBus;
import com.pvz.system.event.CombatListener;
import com.pvz.system.event.PlantEatenEvent;
import com.pvz.system.event.SunCollectedEvent;
import com.pvz.system.resource.ResourceManager;
import javafx.scene.media.AudioClip;

/**
 * Singleton sound-effect manager backed by JavaFX {@link AudioClip} and
 * {@link ResourceManager}. Supports Observer-based automatic SFX triggers via
 * {@link CombatEventBus}.
 */
public final class SoundManager {
    public static final String SFX_SHOOT = "/assets/audio/sfx/shoot.wav";
    public static final String SFX_SPLAT = "/assets/audio/sfx/splat.wav";
    public static final String SFX_CHOMP = "/assets/audio/sfx/chomp.wav";
    public static final String SFX_BOOM = "/assets/audio/sfx/boom.wav";
    public static final String SFX_SUN = "/assets/audio/sfx/sun.wav";

    private static final SoundManager INSTANCE = new SoundManager();

    private boolean muted = false;
    private double volume = 0.7;

    private final CombatListener<BulletHitEvent> bulletHitListener = event -> playSplat();
    private final CombatListener<BombExplosionEvent> bombExplosionListener = event -> playBoom();
    private final CombatListener<PlantEatenEvent> plantEatenListener = event -> playChomp();
    private final CombatListener<SunCollectedEvent> sunCollectedListener = event -> playSunCollected();

    private SoundManager() {
    }

    public static SoundManager getInstance() {
        return INSTANCE;
    }

    /**
     * Preloads all default SFX clips into {@link ResourceManager}'s RAM cache.
     */
    public void preloadDefaults() {
        ResourceManager.getInstance().preloadAudio(
                SFX_SHOOT,
                SFX_SPLAT,
                SFX_CHOMP,
                SFX_BOOM,
                SFX_SUN
        );
    }

    /**
     * Plays a sound effect from the given classpath resource path.
     * Safely no-ops if muted, missing, or running in a headless environment.
     */
    public void playSfx(String path) {
        if (muted || path == null || path.isBlank()) {
            return;
        }
        AudioClip clip = ResourceManager.getInstance().getAudioClip(path);
        if (clip == null) {
            return;
        }
        try {
            clip.play(volume);
        } catch (Exception | LinkageError ignored) {
            // Safe fallback when audio device or JavaFX toolkit is unavailable
        }
    }

    public void playShoot() {
        playSfx(SFX_SHOOT);
    }

    public void playSplat() {
        playSfx(SFX_SPLAT);
    }

    public void playChomp() {
        playSfx(SFX_CHOMP);
    }

    public void playBoom() {
        playSfx(SFX_BOOM);
    }

    public void playSunCollected() {
        playSfx(SFX_SUN);
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public boolean isMuted() {
        return muted;
    }

    public void setVolume(double volume) {
        if (Double.isNaN(volume)) {
            return;
        }
        this.volume = Math.max(0.0, Math.min(1.0, volume));
    }

    public double getVolume() {
        return volume;
    }

    /**
     * Subscribes this manager to combat events on the provided bus.
     */
    public void registerListeners(CombatEventBus bus) {
        if (bus == null) {
            return;
        }
        bus.subscribe(BulletHitEvent.class, bulletHitListener);
        bus.subscribe(BombExplosionEvent.class, bombExplosionListener);
        bus.subscribe(PlantEatenEvent.class, plantEatenListener);
        bus.subscribe(SunCollectedEvent.class, sunCollectedListener);
    }

    /**
     * Unsubscribes this manager from combat events on the provided bus.
     */
    public void unregisterListeners(CombatEventBus bus) {
        if (bus == null) {
            return;
        }
        bus.unsubscribe(BulletHitEvent.class, bulletHitListener);
        bus.unsubscribe(BombExplosionEvent.class, bombExplosionListener);
        bus.unsubscribe(PlantEatenEvent.class, plantEatenListener);
        bus.unsubscribe(SunCollectedEvent.class, sunCollectedListener);
    }
}
