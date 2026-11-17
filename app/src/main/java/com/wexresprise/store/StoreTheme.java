package com.wexresprise.store;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;

import com.wexresprise.store.style.StyleConfig;
import com.wexresprise.store.style.StyleManager;

/** Resolves user settings and the active .ws style into one immutable-by-convention palette. */
public final class StoreTheme {

    /** 0 means "system", the rest are ready-to-use ARGB accents. */
    public static final int[] ACCENTS = {
            0,
            0xFFA8C7FA,
            0xFF7BD88F,
            0xFFE8B9FF,
            0xFFFFD28C,
            0xFFFFB4AB,
            0xFF9BE7E0,
            0xFFFFF59D
    };

    private StoreTheme() {
    }

    public static boolean isNight(Context context, Prefs prefs) {
        int mode = prefs.theme();
        if (mode == Prefs.THEME_DARK) {
            return true;
        }
        if (mode == Prefs.THEME_LIGHT) {
            return false;
        }
        return (context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    public static StyleConfig resolve(Context context, Prefs prefs) {
        boolean night = isNight(context, prefs);
        StyleConfig s = new StyleConfig();
        s.night = night;

        int accent = prefs.accent();
        if (accent == 0) {
            accent = systemAccent(context, night);
        }
        s.setAccent(accent);

        if (night) {
            s.background = 0xFF0B0D12;
            s.surface = 0xFF141820;
            s.text = 0xFFE7EAF0;
            s.subtext = 0xFF9BA5B4;
            s.outline = 0x33FFFFFF;
            s.cardFill = 0x14FFFFFF;
            s.cardStroke = 0x2EFFFFFF;
        } else {
            s.background = 0xFFF4F6FB;
            s.surface = 0xFFFFFFFF;
            s.text = 0xFF151A22;
            s.subtext = 0xFF5B6472;
            s.outline = 0x1F000000;
            s.cardFill = 0xB8FFFFFF;
            s.cardStroke = 0x24000000;
        }

        s.radius = prefs.radius();
        s.cardRadius = Math.min(34, prefs.radius() + 6);
        s.fontScale = prefs.fontScale() / 100f;
        s.backgroundMode = prefs.background();
        s.cardStyle = prefs.cards();
        s.density = prefs.density();
        s.applyCardStyle();
        s.iconTint = s.accent;
        s.styleId = prefs.styleId();

        try {
            StyleManager.get(context).apply(s);
        } catch (Throwable ignored) {
            // A broken style must never make the store unusable.
        }
        return s;
    }

    public static int systemAccent(Context c, boolean night) {
        if (Build.VERSION.SDK_INT >= 31) {
            int res = night ? android.R.color.system_accent1_200
                    : android.R.color.system_accent1_600;
            try {
                int color = c.getColor(res);
                if (Color.alpha(color) == 255) {
                    return color;
                }
            } catch (Throwable ignored) {
            }
        }
        return c.getColor(R.color.wx_primary);
    }

    /** The accent to use for a compact settings swatch. */
    public static int displayAccent(Context c, Prefs prefs) {
        int accent = prefs.accent();
        if (accent == 0) {
            accent = systemAccent(c, isNight(c, prefs));
        }
        return accent;
    }
}
