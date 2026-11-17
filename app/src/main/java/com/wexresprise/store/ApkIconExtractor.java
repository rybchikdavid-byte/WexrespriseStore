package com.wexresprise.store;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/** Reads the launcher icon out of an APK without installing it. */
public final class ApkIconExtractor {

    private static final int ICON_SIZE = 192;

    private ApkIconExtractor() {
    }

    /**
     * Returns a PNG in the cache directory, or null when the APK has no readable icon.
     * The caller owns the returned file.
     */
    public static File extractAndSave(Context context, Uri apkUri) {
        if (apkUri == null) {
            return null;
        }
        File apk = null;
        boolean temporary = false;
        try {
            File dir = new File(context.getCacheDir(), "apk-icons");
            if (!dir.exists() && !dir.mkdirs()) {
                return null;
            }
            File icon = new File(dir, "icon-" + System.currentTimeMillis() + ".png");

            if ("file".equalsIgnoreCase(apkUri.getScheme()) && apkUri.getPath() != null) {
                apk = new File(apkUri.getPath());
            } else {
                apk = File.createTempFile("icon-source-", ".apk", context.getCacheDir());
                temporary = true;
                try (InputStream in = context.getContentResolver().openInputStream(apkUri)) {
                    if (in == null) {
                        return null;
                    }
                    try (OutputStream out = new FileOutputStream(apk)) {
                        copy(in, out);
                    }
                }
            }

            Bitmap bitmap = loadIcon(context, apk);
            if (bitmap == null) {
                return null;
            }
            try (OutputStream out = new FileOutputStream(icon)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            }
            bitmap.recycle();
            return icon;
        } catch (Throwable ignored) {
            return null;
        } finally {
            if (temporary && apk != null) {
                //noinspection ResultOfMethodCallIgnored
                apk.delete();
            }
        }
    }

    private static Bitmap loadIcon(Context context, File apk) {
        PackageManager pm = context.getPackageManager();
        PackageInfo info = pm.getPackageArchiveInfo(apk.getAbsolutePath(), 0);
        if (info == null || info.applicationInfo == null) {
            return null;
        }
        ApplicationInfo app = info.applicationInfo;
        app.sourceDir = apk.getAbsolutePath();
        app.publicSourceDir = apk.getAbsolutePath();
        try {
            Drawable drawable = pm.getApplicationIcon(app);
            if (drawable == null) {
                return null;
            }
            Bitmap bitmap = Bitmap.createBitmap(ICON_SIZE, ICON_SIZE, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(Color.TRANSPARENT);
            drawable.setBounds(0, 0, ICON_SIZE, ICON_SIZE);
            drawable.draw(canvas);
            return bitmap;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[64 * 1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }
}
