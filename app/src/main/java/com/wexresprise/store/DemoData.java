package com.wexresprise.store;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

public final class DemoData {

    private DemoData() {
    }

    public static List<StoreItem> items(Context c) {
        List<StoreItem> list = new ArrayList<>();
        list.add(item("demo-launcher",
                "Wexresprise Launcher 1.0",
                c.getString(R.string.demo_launcher_desc),
                204644L,
                "application/vnd.android.package-archive",
                "WexrespriseLauncher.apk",
                "2026-11-15T09:40:00+00:00",
                true));
        list.add(item("demo-store-icon",
                "Wexresprise Store — иконка",
                c.getString(R.string.demo_icon_desc),
                3937L,
                "image/png",
                "wexresprise-store.png",
                "2026-11-15T09:35:00+00:00",
                true));
        list.add(item("demo-projects",
                "Wexresprise Projects — README",
                c.getString(R.string.demo_readme_desc),
                336L,
                "text/plain",
                "Wexresprise-Projects.txt",
                "2026-11-15T09:30:00+00:00",
                true));
        list.add(item("demo-wallpaper",
                "Обои Aurora",
                c.getString(R.string.demo_wallpaper_desc),
                71215L,
                "image/png",
                "sample-wallpaper.png",
                "2026-11-15T09:20:00+00:00",
                false));
        list.add(item("demo-note",
                "Заметка сообщества",
                c.getString(R.string.demo_note_desc),
                186L,
                "text/plain",
                "sample-note.txt",
                "2026-11-15T09:10:00+00:00",
                false));
        return list;
    }

    private static StoreItem item(String id, String title, String description, long size,
                                  String mime, String file, String createdAt, boolean wex) {
        return new StoreItem(id, title, description, size, mime, file,
                StoreConfig.DEMO_BASE + file, createdAt, wex);
    }
}
