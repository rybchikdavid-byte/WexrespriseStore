package com.wexresprise.store;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.TextView;

import com.wexresprise.store.style.StyleConfig;
import com.wexresprise.store.style.StyleManager;

/**
 * Shared shell for the store screens: resolves the appearance, applies the light/dark platform
 * theme before inflation, draws the selected background and provides the small view helpers.
 */
public abstract class BaseStoreActivity extends Activity {

    protected Prefs prefs;
    protected StyleConfig style;
    protected int accent;
    protected int onAccent;
    protected int textColor;
    protected int subColor;

    private int appliedStyleVersion;
    private boolean crashScreenShown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            prefs = new Prefs(this);
        } catch (Throwable t) {
            prefs = null;
            CrashLog.record(this, t);
        }
        style = StoreTheme.resolve(this, prefs);
        try {
            setTheme(style.night ? R.style.Theme_WexrespriseStore
                    : R.style.Theme_WexrespriseStore_Light);
        } catch (Throwable ignored) {
        }
        super.onCreate(savedInstanceState);
        accent = style.accent;
        onAccent = style.onAccent;
        textColor = style.text;
        subColor = style.subtext;
        appliedStyleVersion = prefs == null ? 0 : prefs.styleVersion();
        try {
            edgeToEdge();
        } catch (Throwable t) {
            CrashLog.record(this, t);
        }
        try {
            updateSystemBars();
        } catch (Throwable t) {
            CrashLog.record(this, t);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (!crashScreenShown) {
            showPreviousCrash();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (prefs == null) {
            return;
        }
        try {
            if (prefs.styleVersion() != appliedStyleVersion) {
                appliedStyleVersion = prefs.styleVersion();
                recreate();
            }
        } catch (Throwable t) {
            CrashLog.record(this, t);
        }
    }

    /**
     * Replaces the screen with a plain crash report. Used when building the normal interface
     * throws, so the reason stays readable on the phone instead of just closing the app.
     */
    protected void showCrashScreen(Throwable error) {
        crashScreenShown = true;
        String trace = CrashLog.traceOf(error);
        try {
            setContentView(CrashScreen.build(this, trace, new Runnable() {
                @Override
                public void run() {
                    if (prefs != null) {
                        prefs.resetAppearance();
                    }
                    try {
                        StyleManager.get(BaseStoreActivity.this).select("");
                    } catch (Throwable ignored) {
                    }
                    recreate();
                }
            }));
        } catch (Throwable ignored) {
        }
    }

    private void showPreviousCrash() {
        String trace;
        try {
            trace = CrashLog.take(this);
        } catch (Throwable t) {
            return;
        }
        if (trace.trim().isEmpty()) {
            return;
        }
        final String stored = trace;
        try {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.crash_previous)
                    .setMessage(CrashLog.summary(stored) + "\n\n" + getString(R.string.crash_hint))
                    .setPositiveButton(R.string.crash_details, (dialog, which) ->
                            startActivity(CrashActivity.intent(this, stored)))
                    .setNeutralButton(R.string.crash_reset, (dialog, which) -> {
                        if (prefs != null) {
                            prefs.resetAppearance();
                        }
                        try {
                            StyleManager.get(this).select("");
                        } catch (Throwable ignored) {
                        }
                        recreate();
                    })
                    .setNegativeButton(R.string.crash_close, null)
                    .show();
        } catch (Throwable ignored) {
        }
    }

    protected void edgeToEdge() {
        Window w = getWindow();
        w.setStatusBarColor(Color.TRANSPARENT);
        w.setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= 30) {
            w.setDecorFitsSystemWindows(false);
        } else {
            w.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    }

    protected void updateSystemBars() {
        if (style == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller == null) {
                return;
            }
            int mask = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
            controller.setSystemBarsAppearance(style.night ? 0 : mask, mask);
        } else {
            int flags = getWindow().getDecorView().getSystemUiVisibility();
            flags &= ~(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
            if (!style.night) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                        | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            getWindow().getDecorView().setSystemUiVisibility(flags);
        }
    }

    /** A full-screen background matching the user's appearance settings. */
    protected View buildBackground() {
        if (style.backgroundMode == StyleConfig.BACKGROUND_AURORA) {
            AuroraView aurora = new AuroraView(this);
            aurora.setAccent(accent);
            aurora.setNight(style.night);
            return aurora;
        }
        View view = new View(this);
        if (style.backgroundMode == StyleConfig.BACKGROUND_GRADIENT) {
            GradientDrawable gradient = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{style.background, blend(style.background, accent, 0.14f)});
            view.setBackground(gradient);
        } else {
            view.setBackground(new ColorDrawable(style.background));
        }
        return view;
    }

    private static int blend(int a, int b, float t) {
        int r = (int) (((a >> 16) & 0xFF) + (((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * t);
        int g = (int) (((a >> 8) & 0xFF) + (((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * t);
        int bl = (int) ((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    protected int dp(float value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                getResources().getDisplayMetrics()));
    }

    /** Text size in sp, already multiplied by the user's font scale. */
    protected float sp(float value) {
        return value * (style == null ? 1f : style.fontScale);
    }

    protected TextView label(CharSequence text, float sizeSp, int color, boolean medium) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp(sizeSp));
        v.setTextColor(color);
        if (medium) {
            v.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        }
        return v;
    }

    protected GlassDrawable glass(float radius, int fill, int stroke, boolean gloss) {
        GlassDrawable d = new GlassDrawable(dp(radius), fill, stroke, Math.max(1f,
                getResources().getDisplayMetrics().density));
        d.setGloss(gloss);
        return d;
    }

    protected GlassDrawable cardBackground() {
        float radius = style.cardRadius;
        GlassDrawable d = glass(radius, style.cardFill, style.cardStroke,
                style.cardStyle == StyleConfig.CARDS_GLASS);
        if (style.cardStyle == StyleConfig.CARDS_GLASS) {
            d.setGlossAlpha(style.night ? 0x30 : 0x70);
        }
        return d;
    }

    protected Drawable accentFill(float radius) {
        return glass(radius, accent, BadgeView.withAlpha(accent, 60), false);
    }

    protected int screenBottomInset(WindowInsets insets) {
        if (Build.VERSION.SDK_INT >= 30) {
            return insets.getInsets(WindowInsets.Type.systemBars()).bottom;
        }
        return insets.getSystemWindowInsetBottom();
    }

    protected int screenTopInset(WindowInsets insets) {
        if (Build.VERSION.SDK_INT >= 30) {
            return insets.getInsets(WindowInsets.Type.systemBars()).top;
        }
        return insets.getSystemWindowInsetTop();
    }

    protected boolean compact() {
        return style != null && style.density == StyleConfig.DENSITY_COMPACT;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateSystemBars();
    }
}
