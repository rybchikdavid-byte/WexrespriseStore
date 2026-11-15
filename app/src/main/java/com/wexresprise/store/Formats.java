package com.wexresprise.store;

import android.graphics.Color;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class Formats {

    private Formats() {
    }

    public static String size(long bytes) {
        if (bytes <= 0) {
            return "—";
        }
        if (bytes < 1024) {
            return bytes + " B";
        }
        double kb = bytes / 1024.0;
        if (kb < 1024) {
            return trim(kb) + " KB";
        }
        double mb = kb / 1024.0;
        if (mb < 1024) {
            return trim(mb) + " MB";
        }
        return trim(mb / 1024.0) + " GB";
    }

    private static String trim(double v) {
        if (v >= 100) {
            return String.format(Locale.ROOT, "%.0f", v);
        }
        if (v >= 10) {
            return String.format(Locale.ROOT, "%.1f", v);
        }
        return String.format(Locale.ROOT, "%.2f", v);
    }

    public static String date(String iso) {
        if (iso == null || iso.isEmpty()) {
            return "";
        }
        try {
            OffsetDateTime time = OffsetDateTime.parse(iso);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.getDefault());
            return fmt.format(time.atZoneSameInstant(ZoneId.systemDefault()));
        } catch (Throwable ignored) {
            return "";
        }
    }

    public static String extensionForMime(String mime) {
        String m = mime == null ? "" : mime.toLowerCase(Locale.ROOT);
        if (m.equals("application/vnd.android.package-archive")) return "apk";
        if (m.equals("application/pdf")) return "pdf";
        if (m.equals("application/zip")) return "zip";
        if (m.equals("application/x-rar-compressed")) return "rar";
        if (m.equals("application/x-7z-compressed")) return "7z";
        if (m.equals("text/plain")) return "txt";
        if (m.equals("text/markdown")) return "md";
        if (m.equals("application/json")) return "json";
        if (m.startsWith("image/")) {
            String s = m.substring(6);
            if (s.equals("jpeg")) return "jpg";
            if (s.equals("svg+xml")) return "svg";
            return s;
        }
        if (m.startsWith("video/")) {
            String s = m.substring(6);
            if (s.equals("quicktime")) return "mov";
            if (s.equals("x-matroska")) return "mkv";
            return s;
        }
        if (m.startsWith("audio/")) {
            String s = m.substring(6);
            if (s.equals("mpeg")) return "mp3";
            if (s.equals("mp4")) return "m4a";
            return s;
        }
        if (m.contains("wordprocessingml")) return "docx";
        if (m.contains("spreadsheetml")) return "xlsx";
        if (m.contains("presentationml")) return "pptx";
        if (m.contains("msword")) return "doc";
        if (m.contains("ms-excel")) return "xls";
        if (m.contains("ms-powerpoint")) return "ppt";
        return "bin";
    }

    public static int contrast(int color) {
        double lum = (0.299 * Color.red(color) + 0.587 * Color.green(color)
                + 0.114 * Color.blue(color)) / 255.0;
        return lum > 0.58 ? 0xFF0A1B33 : 0xFFFFFFFF;
    }

    public static int withAlpha(int color, int percent) {
        int a = Math.max(0, Math.min(100, percent)) * 255 / 100;
        int base = Color.alpha(color);
        return (color & 0x00FFFFFF) | ((base * a / 255) << 24);
    }
}
