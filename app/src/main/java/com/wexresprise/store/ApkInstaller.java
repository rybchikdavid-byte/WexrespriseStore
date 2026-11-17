package com.wexresprise.store;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.core.content.FileProvider;

import java.io.File;

/** Opens the system installer and the "install unknown apps" screen when Android asks for it. */
public final class ApkInstaller {

    private ApkInstaller() {
    }

    public static boolean canInstall(Context context) {
        if (Build.VERSION.SDK_INT < 26) {
            return true;
        }
        try {
            return context.getPackageManager().canRequestPackageInstalls();
        } catch (Throwable ignored) {
            return true;
        }
    }

    /** Returns false when the phone has no screen for the permission. */
    public static boolean openPermissionSettings(Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT < 26) {
            return false;
        }
        try {
            Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + activity.getPackageName()));
            activity.startActivityForResult(intent, requestCode);
            return true;
        } catch (Throwable ignored) {
            try {
                activity.startActivityForResult(
                        new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES), requestCode);
                return true;
            } catch (Throwable ignored2) {
                return false;
            }
        }
    }

    public static boolean install(Activity activity, File apk) {
        if (apk == null || !apk.isFile()) {
            return false;
        }
        try {
            Uri uri = FileProvider.getUriForFile(activity,
                    activity.getPackageName() + ".files", apk);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            activity.startActivity(intent);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
