package com.wexresprise.store;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.webkit.MimeTypeMap;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Talks to Supabase (PostgREST + Storage) with plain HttpURLConnection, no libraries.
 * Falls back to the built-in demo list when no server is configured.
 */
public final class StoreApi {

    public interface ListCallback {
        void onSuccess(List<StoreItem> items);

        void onError(String message);
    }

    public interface CountCallback {
        void onSuccess(int count);

        void onError(String message);
    }

    public interface UploadCallback {
        void onProgress(int percent);

        void onSuccess();

        void onError(String message);
    }

    private static final String SELECT =
            "id,title,description,size_bytes,mime,storage_path,is_wex,created_at";

    private final Context context;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService pool = Executors.newCachedThreadPool();

    public StoreApi(Context c) {
        context = c.getApplicationContext();
    }

    public void load(Prefs prefs, ListCallback cb) {
        if (!prefs.configured()) {
            main.post(() -> cb.onSuccess(DemoData.items(context)));
            return;
        }
        final String base = prefs.url();
        final String key = prefs.key();
        pool.execute(() -> {
            try {
                List<StoreItem> items = fetch(base, key, 200);
                main.post(() -> cb.onSuccess(items));
            } catch (Exception e) {
                main.post(() -> cb.onError(message(e)));
            }
        });
    }

    public void ping(String base, String key, CountCallback cb) {
        final String url = trimSlash(base);
        pool.execute(() -> {
            try {
                int count = count(url, key);
                main.post(() -> cb.onSuccess(count));
            } catch (Exception e) {
                main.post(() -> cb.onError(message(e)));
            }
        });
    }

    /**
     * Uploads the picked file to the storage bucket and inserts a row into public.files.
     */
    public void upload(Prefs prefs, Uri uri, String title, String description,
                       boolean isWex, UploadCallback cb) {
        final String base = prefs.url();
        final String key = prefs.key();
        final ContentResolver resolver = context.getContentResolver();
        final String mime = mimeOf(resolver, uri);
        final long size = sizeOf(resolver, uri);
        final String display = displayName(resolver, uri);
        final String objectName = System.currentTimeMillis() + "-" + safeName(display, mime);
        final String objectPath = encodePath(objectName);

        pool.execute(() -> {
            boolean uploaded = false;
            try {
                InputStream in = resolver.openInputStream(uri);
                if (in == null) {
                    throw new IOException(context.getString(R.string.no_file));
                }
                uploadObject(base, key, objectPath, in, size, mime, percent -> {
                    final int p = percent;
                    main.post(() -> cb.onProgress(p));
                });
                uploaded = true;
                insertRow(base, key, title, description, size, mime, objectName, isWex);
                main.post(cb::onSuccess);
            } catch (Exception e) {
                if (uploaded) {
                    removeObject(base, key, objectPath);
                }
                final String msg = message(e);
                main.post(() -> cb.onError(msg));
            }
        });
    }

    // ---------------------------------------------------------------- reads

    private List<StoreItem> fetch(String base, String key, int limit) throws Exception {
        String q = trimSlash(base) + "/rest/v1/files?select=" + SELECT
                + "&order=created_at.desc&limit=" + limit;
        HttpURLConnection c = open(q, key);
        try {
            int code = c.getResponseCode();
            String body = readAll(code >= 400 ? c.getErrorStream() : c.getInputStream());
            if (code >= 400) {
                throw new IOException(errorFrom(code, body));
            }
            JSONArray arr = new JSONArray(body);
            List<StoreItem> items = new ArrayList<>(arr.length());
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                String path = o.optString("storage_path", "");
                items.add(new StoreItem(
                        o.optString("id", ""),
                        o.optString("title", ""),
                        o.optString("description", ""),
                        o.optLong("size_bytes", 0),
                        o.optString("mime", "application/octet-stream"),
                        path,
                        publicUrl(base, path),
                        o.optString("created_at", ""),
                        o.optBoolean("is_wex", false)));
            }
            return items;
        } finally {
            c.disconnect();
        }
    }

    private int count(String base, String key) throws Exception {
        String q = base + "/rest/v1/files?select=id&limit=1";
        HttpURLConnection c = open(q, key);
        c.setRequestProperty("Prefer", "count=exact");
        try {
            int code = c.getResponseCode();
            String body = readAll(code >= 400 ? c.getErrorStream() : c.getInputStream());
            if (code >= 400) {
                throw new IOException(errorFrom(code, body));
            }
            String range = c.getHeaderField("Content-Range");
            if (range != null) {
                int slash = range.lastIndexOf('/');
                if (slash >= 0) {
                    String total = range.substring(slash + 1).trim();
                    if (!total.isEmpty() && !"*".equals(total)) {
                        try {
                            return Integer.parseInt(total);
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }
            return new JSONArray(body).length();
        } finally {
            c.disconnect();
        }
    }

    // --------------------------------------------------------------- writes

    private void uploadObject(String base, String key, String encodedPath,
                              InputStream source, long size, String mime, Progress progress)
            throws Exception {
        String url = trimSlash(base) + "/storage/v1/object/" + StoreConfig.BUCKET + "/" + encodedPath;
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        try {
            c.setRequestMethod("POST");
            c.setDoOutput(true);
            c.setConnectTimeout(20000);
            c.setReadTimeout(90000);
            c.setRequestProperty("apikey", key);
            c.setRequestProperty("Authorization", "Bearer " + key);
            c.setRequestProperty("Content-Type", mime);
            c.setRequestProperty("Cache-Control", "3600");
            c.setRequestProperty("x-upsert", "false");
            if (size > 0) {
                c.setFixedLengthStreamingMode(size);
            } else {
                c.setChunkedStreamingMode(64 * 1024);
            }

            long total = size > 0 ? size : -1;
            long sent = 0;
            int lastPercent = -1;
            byte[] buffer = new byte[64 * 1024];
            try (InputStream in = new BufferedInputStream(source, 64 * 1024);
                 OutputStream out = new BufferedOutputStream(c.getOutputStream(), 64 * 1024)) {
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                    sent += read;
                    if (total > 0) {
                        int percent = (int) Math.min(99, sent * 100 / total);
                        if (percent != lastPercent) {
                            lastPercent = percent;
                            progress.onProgress(percent);
                        }
                    }
                }
                out.flush();
            }

            int code = c.getResponseCode();
            String body = readAll(code >= 400 ? c.getErrorStream() : c.getInputStream());
            if (code >= 400) {
                throw new IOException(errorFrom(code, body));
            }
            progress.onProgress(100);
        } finally {
            c.disconnect();
        }
    }

    private void insertRow(String base, String key, String title, String description, long size,
                           String mime, String objectName, boolean isWex) throws Exception {
        JSONObject row = new JSONObject();
        row.put("title", title);
        row.put("description", description == null ? "" : description);
        row.put("size_bytes", size);
        row.put("mime", mime);
        row.put("storage_path", objectName);
        row.put("is_wex", isWex);

        String url = trimSlash(base) + "/rest/v1/files";
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        try {
            c.setRequestMethod("POST");
            c.setDoOutput(true);
            c.setConnectTimeout(20000);
            c.setReadTimeout(30000);
            c.setRequestProperty("apikey", key);
            c.setRequestProperty("Authorization", "Bearer " + key);
            c.setRequestProperty("Content-Type", "application/json");
            c.setRequestProperty("Prefer", "return=minimal");
            byte[] payload = row.toString().getBytes(StandardCharsets.UTF_8);
            c.setFixedLengthStreamingMode(payload.length);
            try (OutputStream out = c.getOutputStream()) {
                out.write(payload);
            }
            int code = c.getResponseCode();
            String body = readAll(code >= 300 ? c.getErrorStream() : c.getInputStream());
            if (code >= 300) {
                throw new IOException(errorFrom(code, body));
            }
        } finally {
            c.disconnect();
        }
    }

    private void removeObject(String base, String key, String encodedPath) {
        try {
            String url = trimSlash(base) + "/storage/v1/object/" + StoreConfig.BUCKET + "/"
                    + encodedPath;
            HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
            c.setRequestMethod("DELETE");
            c.setConnectTimeout(10000);
            c.setReadTimeout(10000);
            c.setRequestProperty("apikey", key);
            c.setRequestProperty("Authorization", "Bearer " + key);
            c.getResponseCode();
            c.disconnect();
        } catch (Throwable ignored) {
        }
    }

    // -------------------------------------------------------------- helpers

    private HttpURLConnection open(String url, String key) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestMethod("GET");
        c.setConnectTimeout(20000);
        c.setReadTimeout(30000);
        c.setRequestProperty("apikey", key);
        c.setRequestProperty("Authorization", "Bearer " + key);
        c.setRequestProperty("Accept", "application/json");
        return c;
    }

    private static String publicUrl(String base, String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        return trimSlash(base) + "/storage/v1/object/public/" + StoreConfig.BUCKET + "/"
                + encodePath(path);
    }

    private static String encodePath(String path) {
        StringBuilder out = new StringBuilder();
        String[] parts = path.split("/");
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                out.append('/');
            }
            try {
                out.append(URLEncoder.encode(parts[i], "UTF-8").replace("+", "%20"));
            } catch (Exception e) {
                out.append(parts[i]);
            }
        }
        return out.toString();
    }

    private static String trimSlash(String url) {
        String u = url == null ? "" : url.trim();
        while (u.endsWith("/")) {
            u = u.substring(0, u.length() - 1);
        }
        return u;
    }

    private static String readAll(InputStream in) throws IOException {
        if (in == null) {
            return "";
        }
        try (InputStream stream = in;
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = stream.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toString("UTF-8");
        }
    }

    private String errorFrom(int code, String body) {
        String detail = "";
        try {
            JSONObject o = new JSONObject(body);
            if (o.has("message")) {
                detail = o.optString("message");
            } else if (o.has("error")) {
                detail = o.optString("error");
            } else if (o.has("msg")) {
                detail = o.optString("msg");
            }
        } catch (Throwable ignored) {
            detail = body == null ? "" : body.trim();
        }
        if (detail.length() > 160) {
            detail = detail.substring(0, 160);
        }
        if (code == 413) {
            return context.getString(R.string.err_too_large);
        }
        if (code == 401 || code == 403) {
            return context.getString(R.string.err_forbidden, detail.isEmpty() ? ("HTTP " + code) : detail);
        }
        if (code == 404) {
            return context.getString(R.string.err_not_found, detail.isEmpty() ? ("HTTP " + code) : detail);
        }
        return detail.isEmpty() ? ("HTTP " + code) : detail;
    }

    private String message(Exception e) {
        if (e instanceof java.net.UnknownHostException
                || e instanceof java.net.ConnectException
                || e instanceof java.net.SocketTimeoutException) {
            return context.getString(R.string.err_network);
        }
        String m = e.getMessage();
        if (m == null || m.trim().isEmpty()) {
            return context.getString(R.string.err_network);
        }
        return m;
    }

    private static String mimeOf(ContentResolver r, Uri uri) {
        String mime = null;
        try {
            mime = r.getType(uri);
        } catch (Throwable ignored) {
        }
        if (mime == null || mime.isEmpty()) {
            String name = displayName(r, uri);
            int dot = name.lastIndexOf('.');
            if (dot >= 0) {
                String ext = name.substring(dot + 1).toLowerCase(Locale.ROOT);
                String guessed = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
                if (guessed != null) {
                    mime = guessed;
                }
            }
        }
        return mime == null || mime.isEmpty() ? "application/octet-stream" : mime;
    }

    private static String displayName(ContentResolver r, Uri uri) {
        String name = null;
        try (android.database.Cursor c = r.query(uri,
                new String[]{android.provider.OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                name = c.getString(0);
            }
        } catch (Throwable ignored) {
        }
        if (name == null || name.isEmpty()) {
            name = uri.getLastPathSegment();
        }
        return name == null ? "file" : name;
    }

    private static long sizeOf(ContentResolver r, Uri uri) {
        try (android.database.Cursor c = r.query(uri,
                new String[]{android.provider.OpenableColumns.SIZE}, null, null, null)) {
            if (c != null && c.moveToFirst() && !c.isNull(0)) {
                return Math.max(0, c.getLong(0));
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }

    private static String safeName(String name, String mime) {
        String n = name == null ? "file" : name;
        int slash = Math.max(n.lastIndexOf('/'), n.lastIndexOf('\\'));
        if (slash >= 0) {
            n = n.substring(slash + 1);
        }
        int dot = n.lastIndexOf('.');
        String ext = "";
        if (dot > 0 && dot < n.length() - 1) {
            ext = n.substring(dot);
            n = n.substring(0, dot);
        }
        n = n.replaceAll("[^\\p{L}\\p{N}._-]+", "-");
        while (n.startsWith("-") || n.startsWith(".")) {
            n = n.substring(1);
        }
        while (n.endsWith("-") || n.endsWith(".")) {
            n = n.substring(0, n.length() - 1);
        }
        if (n.isEmpty()) {
            n = "file";
        }
        if (n.length() > 60) {
            n = n.substring(0, 60);
        }
        if (ext.length() > 10 || !ext.matches("\\.[A-Za-z0-9]+")) {
            ext = "." + Formats.extensionForMime(mime);
        }
        return n + ext;
    }

    private interface Progress {
        void onProgress(int percent);
    }
}
