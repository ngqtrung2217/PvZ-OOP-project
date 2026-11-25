package com.pvz.system.resource;

import javafx.scene.image.Image;
import javafx.scene.media.AudioClip;

import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Singleton resource manager that preloads and caches sprite {@link Image}s and
 * low-latency {@link AudioClip}s in RAM.
 * <p>
 * Loading is classpath-based and fault-tolerant: missing or unreadable assets
 * safely return {@code null} instead of throwing runtime exceptions.
 */
public final class ResourceManager {
    private static final ResourceManager INSTANCE = new ResourceManager();

    private final Map<String, Image> imageCache = new HashMap<>();
    private final Map<String, AudioClip> audioCache = new HashMap<>();

    private ResourceManager() {
    }

    public static ResourceManager getInstance() {
        return INSTANCE;
    }

    /**
     * Returns a cached {@link Image} for the given classpath resource path, loading
     * it on first request. Returns {@code null} if the path is invalid, missing,
     * or fails to load.
     */
    public Image getImage(String path) {
        String normalizedPath = normalizePath(path);
        if (normalizedPath == null) {
            return null;
        }
        if (imageCache.containsKey(normalizedPath)) {
            return imageCache.get(normalizedPath);
        }

        URL resourceUrl = getClass().getResource(normalizedPath);
        if (resourceUrl == null) {
            return null;
        }

        try {
            Image image = new Image(resourceUrl.toExternalForm());
            if (image.isError()) {
                return null;
            }
            imageCache.put(normalizedPath, image);
            return image;
        } catch (Exception | LinkageError e) {
            return null;
        }
    }

    /**
     * Returns a cached {@link AudioClip} for the given classpath resource path, loading
     * it on first request. Returns {@code null} if the path is invalid, missing,
     * or fails to load.
     */
    public AudioClip getAudioClip(String path) {
        String normalizedPath = normalizePath(path);
        if (normalizedPath == null) {
            return null;
        }
        if (audioCache.containsKey(normalizedPath)) {
            return audioCache.get(normalizedPath);
        }

        URL resourceUrl = getClass().getResource(normalizedPath);
        if (resourceUrl == null) {
            return null;
        }

        try {
            AudioClip clip = new AudioClip(resourceUrl.toExternalForm());
            audioCache.put(normalizedPath, clip);
            return clip;
        } catch (Exception | LinkageError e) {
            return null;
        }
    }

    /**
     * Preloads multiple image assets into the cache.
     */
    public void preloadImages(String... paths) {
        if (paths == null) {
            return;
        }
        for (String path : paths) {
            getImage(path);
        }
    }

    /**
     * Preloads multiple image assets into the cache.
     */
    public void preloadImages(Collection<String> paths) {
        if (paths == null) {
            return;
        }
        for (String path : paths) {
            getImage(path);
        }
    }

    /**
     * Preloads multiple audio clips into the cache.
     */
    public void preloadAudio(String... paths) {
        if (paths == null) {
            return;
        }
        for (String path : paths) {
            getAudioClip(path);
        }
    }

    /**
     * Preloads multiple audio clips into the cache.
     */
    public void preloadAudio(Collection<String> paths) {
        if (paths == null) {
            return;
        }
        for (String path : paths) {
            getAudioClip(path);
        }
    }

    /**
     * Clears all cached images and audio clips.
     */
    public void clearCache() {
        imageCache.clear();
        audioCache.clear();
    }

    public int getCachedImageCount() {
        return imageCache.size();
    }

    public int getCachedAudioCount() {
        return audioCache.size();
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String trimmed = path.trim();
        return trimmed.startsWith("/") ? trimmed : "/" + trimmed;
    }
}
