package com.wexresprise.store.style;

/**
 * Entry point of a Java style packaged in a .ws file.
 *
 * <p>The class named in {@code manifest.json} must implement this interface and have a public
 * no-argument constructor. It runs inside Wexresprise Store with the app's permissions, so a
 * .ws file is executable code: install only styles you trust.</p>
 *
 * <pre>
 * public class AuroraStyle implements WexStyle {
 *     public void apply(StyleConfig s) {
 *         s.setAccent(0xFF7BD88F);
 *         s.cardRadius = 26f;
 *         s.backgroundMode = StyleConfig.BACKGROUND_AURORA;
 *     }
 * }
 * </pre>
 */
public interface WexStyle {

    /**
     * Called on a background-free path when an activity is prepared. Assign any public field of
     * {@code style}; do not keep the object after returning.
     */
    void apply(StyleConfig style);
}
