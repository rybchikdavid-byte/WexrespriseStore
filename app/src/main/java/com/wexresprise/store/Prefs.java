package com.wexresprise.store;

import android.content.Context;
import android.content.SharedPreferences;

public class Prefs {

    public static final int THEME_SYSTEM = 0;
    public static final int THEME_LIGHT = 1;
    public static final int THEME_DARK = 2;

    private final SharedPreferences sp;

    public Prefs(Context c) {
        sp = c.getSharedPreferences("wex_store", Context.MODE_PRIVATE);
    }

    public String url() {
        return sp.getString("server_url", StoreConfig.DEFAULT_URL).trim();
    }

    public String key() {
        return sp.getString("server_key", StoreConfig.DEFAULT_KEY).trim();
    }

    public boolean configured() {
        return !url().isEmpty() && !key().isEmpty();
    }

    public void setServer(String url, String key) {
        String u = url == null ? "" : url.trim();
        while (u.endsWith("/")) {
            u = u.substring(0, u.length() - 1);
        }
        sp.edit()
                .putString("server_url", u)
                .putString("server_key", key == null ? "" : key.trim())
                .apply();
    }

    public void clearServer() {
        sp.edit().remove("server_url").remove("server_key").apply();
    }

    // ------------------------------------------------------------- appearance

    /** Accent ARGB. 0 means "follow the system". */
    public int accent() {
        return sp.getInt("accent", 0);
    }

    public void setAccent(int color) {
        sp.edit().putInt("accent", color).apply();
        bumpStyleVersion();
    }

    public int theme() {
        return sp.getInt("theme", THEME_SYSTEM);
    }

    public void setTheme(int theme) {
        sp.edit().putInt("theme", theme).apply();
        bumpStyleVersion();
    }

    public int background() {
        return sp.getInt("background", 0);
    }

    public void setBackground(int background) {
        sp.edit().putInt("background", background).apply();
        bumpStyleVersion();
    }

    public int cards() {
        return sp.getInt("cards", 0);
    }

    public void setCards(int cards) {
        sp.edit().putInt("cards", cards).apply();
        bumpStyleVersion();
    }

    /** Corner radius in dp, 0–28. */
    public int radius() {
        return sp.getInt("radius", 22);
    }

    public void setRadius(int radius) {
        sp.edit().putInt("radius", Math.max(0, Math.min(28, radius))).apply();
        bumpStyleVersion();
    }

    /** Text scale in percent, 85–125. */
    public int fontScale() {
        return sp.getInt("font_scale", 100);
    }

    public void setFontScale(int percent) {
        sp.edit().putInt("font_scale", Math.max(85, Math.min(125, percent))).apply();
        bumpStyleVersion();
    }

    public int density() {
        return sp.getInt("density", 0);
    }

    public void setDensity(int density) {
        sp.edit().putInt("density", density).apply();
        bumpStyleVersion();
    }

    /** Launcher icon variant: 0 green, 1 blue, 2 violet, 3 orange, 4 pink, 5 teal. */
    public int icon() {
        return sp.getInt("icon", 0);
    }

    public void setIcon(int icon) {
        sp.edit().putInt("icon", Math.max(0, Math.min(5, icon))).apply();
    }

    // ------------------------------------------------------------------ styles

    public String styleId() {
        return sp.getString("style_id", "").trim();
    }

    public void setStyleId(String id) {
        sp.edit().putString("style_id", id == null ? "" : id.trim()).apply();
        bumpStyleVersion();
    }

    public int styleVersion() {
        return sp.getInt("style_version", 0);
    }

    public void bumpStyleVersion() {
        sp.edit().putInt("style_version", styleVersion() + 1).apply();
    }

    public void resetAppearance() {
        sp.edit()
                .remove("accent")
                .remove("theme")
                .remove("background")
                .remove("cards")
                .remove("radius")
                .remove("font_scale")
                .remove("density")
                .remove("style_id")
                .apply();
        bumpStyleVersion();
    }
}
