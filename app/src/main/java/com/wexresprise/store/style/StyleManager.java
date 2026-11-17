package com.wexresprise.store.style;

import android.content.Context;
import android.net.Uri;

import com.wexresprise.store.Prefs;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import dalvik.system.DexClassLoader;

/**
 * Installs, stores and loads .ws styles.
 *
 * <p>A .ws file is a ZIP with {@code manifest.json}, at least one {@code classes*.dex} and an
 * optional {@code icon.png}. Styles are executable code and run with the app's permissions.</p>
 */
public final class StyleManager {

    private static final long MAX_ARCHIVE = 24L * 1024 * 1024;
    private static final long MAX_UNPACKED = 20L * 1024 * 1024;
    private static final int MAX_ENTRIES = 200;
    private static final long MAX_MANIFEST = 64L * 1024;

    private static StyleManager instance;

    private final Context context;
    private final Prefs prefs;
    private final File root;

    private String cachedId;
    private WexStyle cached;
    private String lastError = "";

    private StyleManager(Context c) {
        context = c.getApplicationContext();
        prefs = new Prefs(context);
        root = new File(context.getFilesDir(), "styles");
    }

    public static synchronized StyleManager get(Context c) {
        if (instance == null) {
            instance = new StyleManager(c);
        }
        return instance;
    }

    public String lastError() {
        return lastError == null ? "" : lastError;
    }

    public List<InstalledStyle> installed() {
        List<InstalledStyle> out = new ArrayList<>();
        File[] dirs = root.listFiles();
        if (dirs == null) {
            return out;
        }
        for (File dir : dirs) {
            if (!dir.isDirectory() || dir.getName().startsWith(".")) {
                continue;
            }
            try {
                InstalledStyle style = readManifest(dir);
                if (style != null) {
                    out.add(style);
                }
            } catch (Throwable ignored) {
            }
        }
        Collections.sort(out, new Comparator<InstalledStyle>() {
            @Override
            public int compare(InstalledStyle a, InstalledStyle b) {
                return a.name.compareToIgnoreCase(b.name);
            }
        });
        return out;
    }

    public InstalledStyle find(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        for (InstalledStyle style : installed()) {
            if (style.id.equals(id)) {
                return style;
            }
        }
        return null;
    }

    public InstalledStyle selected() {
        return find(prefs.styleId());
    }

    /** Applies the selected style, if any. Never throws. */
    public void apply(StyleConfig config) {
        if (config == null) {
            return;
        }
        InstalledStyle installed = selected();
        if (installed == null) {
            return;
        }
        WexStyle style = load(installed);
        if (style == null) {
            return;
        }
        config.styleId = installed.id;
        File assets = new File(installed.dir, "assets");
        config.assetsDir = assets.isDirectory() ? assets.getAbsolutePath() : null;
        try {
            style.apply(config);
        } catch (Throwable t) {
            lastError = safeMessage(t);
        }
    }

    private WexStyle load(InstalledStyle installed) {
        if (installed == null) {
            return null;
        }
        if (installed.id.equals(cachedId)) {
            return cached;
        }
        try {
            WexStyle style = instantiate(installed);
            cached = style;
            cachedId = installed.id;
            lastError = "";
            return style;
        } catch (Throwable t) {
            cached = null;
            cachedId = installed.id;
            lastError = safeMessage(t);
            return null;
        }
    }

    private WexStyle instantiate(InstalledStyle style) throws Exception {
        if (style.className.isEmpty()) {
            throw new IOException("manifest.json has no class");
        }
        File[] found = style.dir.listFiles((dir, name) ->
                name.matches("classes[0-9]*\\.dex"));
        if (found == null || found.length == 0) {
            throw new IOException("classes.dex is missing");
        }
        Arrays.sort(found, new Comparator<File>() {
            @Override
            public int compare(File a, File b) {
                return a.getName().compareTo(b.getName());
            }
        });

        StringBuilder path = new StringBuilder();
        for (File dex : found) {
            // Android 14 refuses to load writable dex files.
            if (!dex.setReadOnly() && dex.canWrite()) {
                throw new IOException("cannot make " + dex.getName() + " read-only");
            }
            if (path.length() > 0) {
                path.append(File.pathSeparator);
            }
            path.append(dex.getAbsolutePath());
        }

        DexClassLoader loader = new DexClassLoader(path.toString(), null, null,
                WexStyle.class.getClassLoader());
        Class<?> cls = Class.forName(style.className, true, loader);
        if (!WexStyle.class.isAssignableFrom(cls)) {
            throw new IOException(style.className + " does not implement WexStyle");
        }
        Object object = cls.getDeclaredConstructor().newInstance();
        return (WexStyle) object;
    }

    /**
     * Installs a .ws file and selects it. Throws an IOException with a message suitable for a
     * dialog when the archive is malformed or its class cannot be loaded.
     */
    public InstalledStyle install(Uri uri) throws IOException {
        if (uri == null) {
            throw new IOException("no file");
        }
        if (!root.exists() && !root.mkdirs()) {
            throw new IOException("cannot create the styles directory");
        }

        File archive = File.createTempFile("wex-style-", ".ws", context.getCacheDir());
        File tempDir = new File(root, ".import-" + System.currentTimeMillis());
        try {
            copyUri(uri, archive);

            ZipFile zip = new ZipFile(archive);
            try {
                List<? extends ZipEntry> entries = Collections.list(zip.entries());
                if (entries.size() > MAX_ENTRIES) {
                    throw new IOException("too many files in the archive");
                }
                boolean hasManifest = false;
                boolean hasDex = false;
                for (ZipEntry entry : entries) {
                    String name = entry.getName().replace('\\', '/');
                    if (name.equals("manifest.json")) {
                        hasManifest = true;
                    }
                    if (name.matches("classes[0-9]*\\.dex")) {
                        hasDex = true;
                    }
                }
                if (!hasManifest) {
                    throw new IOException("manifest.json is missing");
                }
                if (!hasDex) {
                    throw new IOException("classes.dex is missing (compile the style to dex)");
                }
                if (!tempDir.mkdirs()) {
                    throw new IOException("cannot unpack the style");
                }

                long total = 0;
                for (ZipEntry entry : entries) {
                    String name = safeName(entry.getName());
                    if (name.isEmpty()) {
                        continue;
                    }
                    File out = new File(tempDir, name);
                    String base = tempDir.getCanonicalPath();
                    if (!out.getCanonicalPath().startsWith(base + File.separator)) {
                        throw new IOException("unsafe path in the archive: " + name);
                    }
                    if (entry.isDirectory()) {
                        if (!out.mkdirs() && !out.isDirectory()) {
                            throw new IOException("cannot create " + name);
                        }
                        continue;
                    }
                    File parent = out.getParentFile();
                    if (parent != null && !parent.exists() && !parent.mkdirs()) {
                        throw new IOException("cannot create " + name);
                    }
                    long size = entry.getSize();
                    if (size > 0 && size > MAX_UNPACKED - total) {
                        throw new IOException("style is too large");
                    }
                    long written = copy(zip.getInputStream(entry), out, MAX_UNPACKED - total);
                    total += written;
                    if (total > MAX_UNPACKED) {
                        throw new IOException("style is too large");
                    }
                }
            } finally {
                zip.close();
            }

            InstalledStyle unpacked = readManifest(tempDir);
            if (unpacked == null) {
                throw new IOException("manifest.json is invalid");
            }
            // Verify the dex and the class before replacing a working style.
            instantiate(unpacked);

            File target = new File(root, unpacked.id);
            deleteTree(target);
            if (!tempDir.renameTo(target)) {
                throw new IOException("cannot finish the installation");
            }
            InstalledStyle installed = readManifest(target);
            if (installed == null) {
                throw new IOException("cannot read the installed style");
            }
            prefs.setStyleId(installed.id);
            cached = null;
            cachedId = null;
            lastError = "";
            return installed;
        } catch (IOException e) {
            throw e;
        } catch (Throwable t) {
            throw new IOException(safeMessage(t));
        } finally {
            deleteTree(tempDir);
            //noinspection ResultOfMethodCallIgnored
            archive.delete();
        }
    }

    public void remove(String id) {
        if (id == null || id.trim().isEmpty()) {
            return;
        }
        if (id.equals(prefs.styleId())) {
            prefs.setStyleId("");
        }
        deleteTree(new File(root, id));
        if (id.equals(cachedId)) {
            cached = null;
            cachedId = null;
        }
    }

    public void select(String id) {
        prefs.setStyleId(id == null ? "" : id);
        cached = null;
        cachedId = null;
    }

    // --------------------------------------------------------------- internals

    private InstalledStyle readManifest(File dir) throws IOException {
        File manifest = new File(dir, "manifest.json");
        if (!manifest.isFile()) {
            return null;
        }
        String text = readText(manifest, MAX_MANIFEST);
        JSONObject json;
        try {
            json = new JSONObject(text);
        } catch (Throwable t) {
            throw new IOException("manifest.json is not valid JSON");
        }
        String id = json.optString("id", "").trim();
        if (id.isEmpty()) {
            id = dir.getName();
        }
        if (!id.matches("[A-Za-z0-9._-]{1,80}")) {
            throw new IOException("invalid style id");
        }
        String className = json.optString("class", "").trim();
        if (className.isEmpty()) {
            throw new IOException("manifest.json has no class");
        }
        if (!className.matches("[A-Za-z_$][A-Za-z0-9_$.]*")) {
            throw new IOException("invalid class name");
        }
        File icon = new File(dir, "icon.png");
        return new InstalledStyle(id,
                json.optString("name", id),
                json.optString("version", ""),
                json.optString("author", ""),
                json.optString("description", ""),
                className,
                dir,
                icon.isFile() ? icon : null);
    }

    private void copyUri(Uri uri, File target) throws IOException {
        InputStream in = context.getContentResolver().openInputStream(uri);
        if (in == null) {
            throw new IOException("cannot read the selected file");
        }
        try (InputStream input = in) {
            copy(input, target, MAX_ARCHIVE);
        }
        if (target.length() > MAX_ARCHIVE) {
            throw new IOException("style is too large");
        }
    }

    private static long copy(InputStream in, File target, long limit) throws IOException {
        long total = 0;
        try (InputStream input = in;
             OutputStream out = new FileOutputStream(target)) {
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > limit) {
                    throw new IOException("style is too large");
                }
                out.write(buffer, 0, read);
            }
        }
        return total;
    }

    private static String readText(File file, long limit) throws IOException {
        try (InputStream in = new java.io.FileInputStream(file);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            long total = 0;
            int read;
            while ((read = in.read(buffer)) != -1) {
                total += read;
                if (total > limit) {
                    throw new IOException("manifest.json is too large");
                }
                out.write(buffer, 0, read);
            }
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }

    private static String safeName(String raw) throws IOException {
        String name = raw == null ? "" : raw.replace('\\', '/').trim();
        while (name.startsWith("./")) {
            name = name.substring(2);
        }
        while (name.startsWith("/")) {
            name = name.substring(1);
        }
        while (name.endsWith("/")) {
            name = name.substring(0, name.length() - 1);
        }
        if (name.isEmpty()) {
            return "";
        }
        String[] parts = name.split("/");
        for (String part : parts) {
            if (part.isEmpty() || part.equals(".") || part.equals("..")) {
                throw new IOException("unsafe path in the archive");
            }
        }
        return name;
    }

    private static void deleteTree(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteTree(child);
            }
        }
        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }

    private static String safeMessage(Throwable t) {
        String message = t == null ? "" : t.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return t == null ? "unknown error" : t.getClass().getSimpleName();
        }
        return message.length() > 220 ? message.substring(0, 220) : message;
    }
}
