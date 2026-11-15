package com.wexresprise.store;

import android.content.Context;
import android.content.SharedPreferences;

public class Prefs {

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
}
