package com.wexresprise.store.style;

/**
 * Everything a .ws style is allowed to change.
 *
 * <p>A style class receives a fully resolved config (the user's settings, already adapted to
 * light or dark) and may overwrite any public field. The values are read once when an activity
 * is created, so call {@code recreate()} after applying a style.</p>
 *
 * <p>The class is part of the public plugin API. Keep the field names and meaning stable:
 * existing .ws files compiled against this class must keep working.</p>
 */
public class StyleConfig {

    public static final int BACKGROUND_AURORA = 0;
    public static final int BACKGROUND_GRADIENT = 1;
    public static final int BACKGROUND_SOLID = 2;

    public static final int CARDS_GLASS = 0;
    public static final int CARDS_SOLID = 1;
    public static final int CARDS_OUTLINE = 2;

    public static final int DENSITY_COMFORTABLE = 0;
    public static final int DENSITY_COMPACT = 1;

    /** True for the dark palette. A style may flip it, but the activity theme is chosen before loading. */
    public boolean night;

    /** Main accent colour (ARGB). */
    public int accent;
    /** Text/icon colour on top of {@link #accent}. */
    public int onAccent;
    /** Window background behind the aurora/gradient. */
    public int background;
    /** Opaque card surface used by the SOLID card style. */
    public int surface;
    /** Primary text. */
    public int text;
    /** Secondary text. */
    public int subtext;
    /** Hairline colour for outline controls. */
    public int outline;
    /** Card fill for the GLASS style. */
    public int cardFill;
    /** Card stroke for the GLASS style. */
    public int cardStroke;

    /** Corner radius, in dp, for buttons and small panels. */
    public float radius;
    /** Corner radius, in dp, for file cards. */
    public float cardRadius;
    /** Text scale, 0.85–1.25. */
    public float fontScale;

    /** One of the BACKGROUND_* constants. */
    public int backgroundMode;
    /** One of the CARDS_* constants. */
    public int cardStyle;
    /** One of the DENSITY_* constants. */
    public int density;

    /** Accent used for the in-app store logo. */
    public int iconTint;

    /** Absolute path to the style's optional assets directory, or null for built-in settings. */
    public String assetsDir;
    /** Id of the applied .ws style, or an empty string for the built-in appearance. */
    public String styleId = "";

    public void setAccent(int color) {
        accent = color;
        onAccent = contrast(color);
        iconTint = color;
    }

    public void applyCardStyle() {
        if (cardStyle == CARDS_SOLID) {
            cardFill = night ? 0xFF1B2029 : 0xFFFFFFFF;
            cardStroke = night ? 0x1FFFFFFF : 0x14000000;
        } else if (cardStyle == CARDS_OUTLINE) {
            cardFill = 0x00000000;
            cardStroke = night ? 0x66FFFFFF : 0x55000000;
        }
    }

    /** Black or white, whichever is readable on the supplied colour. */
    public static int contrast(int color) {
        double lum = (0.299 * ((color >> 16) & 0xFF)
                + 0.587 * ((color >> 8) & 0xFF)
                + 0.114 * (color & 0xFF)) / 255.0;
        return lum > 0.58 ? 0xFF0A1B33 : 0xFFFFFFFF;
    }

    /** A copy with transparent black as alpha base, useful for styles. */
    public static int alpha(int color, int percent) {
        int a = Math.max(0, Math.min(100, percent)) * 255 / 100;
        return (color & 0x00FFFFFF) | (a << 24);
    }
}
