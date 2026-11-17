package com.wexresprise.store;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Small asynchronous loader for the app icons stored next to APKs in Supabase. */
public final class IconLoader {

    private static final int ICON_SIZE = 192;
    private static final int MAX_BYTES = 3 * 1024 * 1024;

    private static IconLoader instance;

    private final Context context;
    private final LruCache<String, Bitmap> memory = new LruCache<String, Bitmap>(8 * 1024 * 1024) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount();
        }
    };
    private final ExecutorService pool = Executors.newFixedThreadPool(3);
    private final Handler main = new Handler(Looper.getMainLooper());
    private final File diskDir;

    private IconLoader(Context c) {
        context = c.getApplicationContext();
        diskDir = new File(context.getCacheDir(), "icon-cache");
        if (!diskDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            diskDir.mkdirs();
        }
    }

    public static synchronized IconLoader get(Context c) {
        if (instance == null) {
            instance = new IconLoader(c);
        }
        return instance;
    }

    public void load(String url, ImageView view, int placeholder) {
        if (url == null || url.trim().isEmpty()) {
            view.setTag(null);
            view.setImageResource(placeholder);
            return;
        }
        final String key = url.trim();
        view.setTag(key);
        Bitmap cached = memory.get(key);
        if (cached != null) {
            view.clearColorFilter();
            view.setImageBitmap(cached);
            return;
        }
        view.setImageResource(placeholder);
        pool.execute(() -> {
            Bitmap bitmap = fromDisk(key);
            if (bitmap == null) {
                bitmap = fromNetwork(key);
                if (bitmap != null) {
                    save(key, bitmap);
                }
            }
            if (bitmap != null) {
                memory.put(key, bitmap);
            }
            final Bitmap result = bitmap;
            main.post(() -> {
                if (key.equals(view.getTag())) {
                    if (result != null) {
                        view.clearColorFilter();
                        view.setImageBitmap(result);
                    } else {
                        view.setImageResource(placeholder);
                    }
                }
            });
        });
    }

    private Bitmap fromDisk(String url) {
        File file = diskFile(url);
        if (!file.isFile()) {
            return null;
        }
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            return bitmap == null ? null : square(bitmap);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private Bitmap fromNetwork(String url) {
        try {
            if (url.startsWith("file://")) {
                Bitmap bitmap = BitmapFactory.decodeFile(url.substring("file://".length()));
                return bitmap == null ? null : square(bitmap);
            }
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                return null;
            }
            HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
            try {
                c.setConnectTimeout(12000);
                c.setReadTimeout(20000);
                c.setInstanceFollowRedirects(true);
                c.setRequestProperty("User-Agent", "WexrespriseStore");
                int code = c.getResponseCode();
                if (code < 200 || code >= 300) {
                    return null;
                }
                long length = c.getContentLengthLong();
                if (length > MAX_BYTES) {
                    return null;
                }
                try (InputStream in = c.getInputStream();
                     ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[16 * 1024];
                    int total = 0;
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        total += read;
                        if (total > MAX_BYTES) {
                            return null;
                        }
                        out.write(buffer, 0, read);
                    }
                    byte[] data = out.toByteArray();
                    BitmapFactory.Options bounds = new BitmapFactory.Options();
                    bounds.inJustDecodeBounds = true;
                    BitmapFactory.decodeByteArray(data, 0, data.length, bounds);
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);
                    return bitmap == null ? null : square(bitmap);
                }
            } finally {
                c.disconnect();
            }
        } catch (Throwable ignored) {
            return null;
        }
    }

    private void save(String url, Bitmap bitmap) {
        try (FileOutputStream out = new FileOutputStream(diskFile(url))) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 95, out);
        } catch (Throwable ignored) {
        }
    }

    private File diskFile(String url) {
        return new File(diskDir, sha1(url) + ".png");
    }

    private static int sampleSize(int width, int height) {
        int sample = 1;
        int max = Math.max(width, height);
        while (max / sample > ICON_SIZE * 2) {
            sample *= 2;
        }
        return sample;
    }

    private static Bitmap square(Bitmap source) {
        if (source == null) {
            return null;
        }
        int w = source.getWidth();
        int h = source.getHeight();
        int side = Math.min(w, h);
        int x = (w - side) / 2;
        int y = (h - side) / 2;
        Bitmap cropped = Bitmap.createBitmap(source, x, y, side, side);
        Bitmap scaled = Bitmap.createScaledBitmap(cropped, ICON_SIZE, ICON_SIZE, true);
        if (cropped != source) {
            source.recycle();
        }
        if (scaled != cropped) {
            cropped.recycle();
        }
        return scaled;
    }

    private static String sha1(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] bytes = digest.digest(value.getBytes("UTF-8"));
            StringBuilder out = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                out.append(String.format("%02x", b & 0xFF));
            }
            return out.toString();
        } catch (Throwable ignored) {
            return Integer.toHexString(value.hashCode());
        }
    }
}
