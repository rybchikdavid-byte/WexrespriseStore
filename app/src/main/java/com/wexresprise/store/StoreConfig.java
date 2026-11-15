package com.wexresprise.store;

public final class StoreConfig {

    /** Baked-in Supabase project URL. Leave empty to use demo mode. */
    public static final String DEFAULT_URL = "";

    /** Baked-in Supabase anon public key. Leave empty to use demo mode. */
    public static final String DEFAULT_KEY = "";

    public static final String BUCKET = "wexstore";

    public static final String DEMO_BASE =
            "https://raw.githubusercontent.com/rybchikdavid-byte/WexrespriseStore/main/demo/";

    private StoreConfig() {
    }
}
