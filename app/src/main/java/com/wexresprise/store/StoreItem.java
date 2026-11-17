package com.wexresprise.store;

import java.util.Locale;

public class StoreItem {

    public final String id;
    public final String title;
    public final String description;
    public final String mime;
    public final String storagePath;
    public final String downloadUrl;
    public final String createdAt;
    public final long size;
    public final boolean isWex;
    /** Optional path/URL of the app icon uploaded next to an APK. */
    public final String iconPath;
    public final String iconUrl;

    public StoreItem(String id, String title, String description, long size, String mime,
                     String storagePath, String downloadUrl, String createdAt, boolean isWex) {
        this(id, title, description, size, mime, storagePath, downloadUrl, createdAt, isWex,
                "", "");
    }

    public StoreItem(String id, String title, String description, long size, String mime,
                     String storagePath, String downloadUrl, String createdAt, boolean isWex,
                     String iconPath, String iconUrl) {
        this.id = id == null ? "" : id;
        this.title = title == null ? "" : title;
        this.description = description == null ? "" : description;
        this.size = size;
        this.mime = mime == null || mime.isEmpty() ? "application/octet-stream" : mime;
        this.storagePath = storagePath == null ? "" : storagePath;
        this.downloadUrl = downloadUrl == null ? "" : downloadUrl;
        this.createdAt = createdAt == null ? "" : createdAt;
        this.isWex = isWex;
        this.iconPath = iconPath == null ? "" : iconPath;
        this.iconUrl = iconUrl == null ? "" : iconUrl;
    }

    public boolean isApk() {
        String ext = extension();
        if (ext.isEmpty()) {
            ext = Formats.extensionForMime(mime);
        }
        return ext.equals("apk") || ext.equals("xapk") || ext.equals("apks")
                || mime.equals("application/vnd.android.package-archive");
    }

    public String extension() {
        String p = storagePath;
        int q = p.indexOf('?');
        if (q >= 0) {
            p = p.substring(0, q);
        }
        int dot = p.lastIndexOf('.');
        int slash = p.lastIndexOf('/');
        if (dot > slash && dot < p.length() - 1) {
            return p.substring(dot + 1).toLowerCase(Locale.ROOT);
        }
        return "";
    }

    public boolean belongsToProjects() {
        if (isWex) {
            return true;
        }
        String title = this.title.toLowerCase(Locale.ROOT);
        return title.contains("wexresprise") || title.contains("pixelgram");
    }
}
