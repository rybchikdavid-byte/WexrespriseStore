package com.wexresprise.store;

import android.app.Activity;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = new Prefs(this);
        style = StoreTheme.resolve(this, prefs);
        setTheme(style.night ? R.style.Theme_WexrespriseStore
                : R.style.Theme_WexrespriseStore_Light);
        super.onCreate(savedInstanceState);
        accent = style.accent;
        onAccent = style.onAccent;
        textColor = style.text;
        subColor = style.subtext;
        appliedStyleVersion = prefs.styleVersion();
        edgeToEdge();
        updateSystemBars();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (prefs.styleVersion() != appliedStyleVersion) {
            appliedStyleVersion = prefs.styleVersion();
            recreate();
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
