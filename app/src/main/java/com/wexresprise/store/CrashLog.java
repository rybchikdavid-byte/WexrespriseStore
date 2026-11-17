package com.wexresprise.store;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Process;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Keeps the last uncaught error on disk and opens a plain crash screen in a separate process, so
 * the reason is visible on the phone even when there is no adb and no log access.
 */
public final class CrashLog {

    private static final String PREFS = "wex_crash";
    private static final String KEY = "last";
    private static final String AT = "at";
    private static final int MAX = 24000;

    private static boolean installed;

    private CrashLog() {
    }

    public static void install(final Context context) {
        if (installed) {
            return;
        }
        installed = true;
        final Context app = context.getApplicationContext();
        final Thread.UncaughtExceptionHandler previous =
                Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable error) {
                try {
                    record(app, error);
                } catch (Throwable ignored) {
                }
                try {
                    showCrashScreen(app, error);
                } catch (Throwable ignored) {
                }
                if (previous != null) {
                    previous.uncaughtException(thread, error);
                }
            }
        });
    }

    /** The crash screen runs in its own process and must not install the handler again. */
    public static boolean isCrashProcess(Context context) {
        String name = processName(context);
        return name != null && name.endsWith(":crash");
    }

    private static String processName(Context context) {
        if (Build.VERSION.SDK_INT >= 28) {
            try {
                return Application.getProcessName();
            } catch (Throwable ignored) {
            }
        }
        try {
            ActivityManager am = (ActivityManager) context.getSystemService(
                    Context.ACTIVITY_SERVICE);
            if (am != null) {
                for (ActivityManager.RunningAppProcessInfo info : am.getRunningAppProcesses()) {
                    if (info.pid == Process.myPid()) {
                        return info.processName;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return "";
    }

    public static String traceOf(Throwable error) {
        if (error == null) {
            return "";
        }
        StringWriter out = new StringWriter();
        error.printStackTrace(new PrintWriter(out));
        String trace = out.toString();
        return trace.length() > MAX ? trace.substring(0, MAX) : trace;
    }

    /** A short first line for a dialog. */
    public static String summary(String trace) {
        if (trace == null) {
            return "";
        }
        int end = trace.indexOf('\n');
        String line = end < 0 ? trace : trace.substring(0, end);
        line = line.trim();
        return line.length() > 300 ? line.substring(0, 300) + "…" : line;
    }

    /** Saves the trace and, when possible, also writes it next to the app's files. */
    public static void record(Context context, Throwable error) {
        String trace = traceOf(error);
        if (trace.isEmpty() || context == null) {
            return;
        }
        try {
            SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            sp.edit().putString(KEY, trace).putLong(AT, System.currentTimeMillis()).commit();
        } catch (Throwable ignored) {
        }
        try {
            File dir = context.getExternalFilesDir(null);
            if (dir != null && (dir.exists() || dir.mkdirs())) {
                File file = new File(dir, "last-crash.txt");
                try (Writer writer = new FileWriter(file, false)) {
                    writer.write(trace);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    /** The stored trace without clearing it. */
    public static String peek(Context context) {
        if (context == null) {
            return "";
        }
        try {
            SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String trace = sp.getString(KEY, "");
            return trace == null ? "" : trace;
        } catch (Throwable ignored) {
            return "";
        }
    }

    /** The stored trace, cleared after reading. */
    public static String take(Context context) {
        String trace = peek(context);
        if (!trace.trim().isEmpty()) {
            clear(context);
        }
        return trace;
    }

    public static void clear(Context context) {
        if (context == null) {
            return;
        }
        try {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit().remove(KEY).remove(AT).commit();
        } catch (Throwable ignored) {
        }
    }

    /** The copy left in the app's external files folder, if any. */
    public static String readFile(Context context) {
        if (context == null) {
            return "";
        }
        try {
            File dir = context.getExternalFilesDir(null);
            File file = dir == null ? null : new File(dir, "last-crash.txt");
            if (file == null || !file.isFile() || file.length() > MAX * 4L) {
                return "";
            }
            byte[] data = new byte[(int) file.length()];
            try (FileInputStream in = new FileInputStream(file)) {
                int read = 0;
                while (read < data.length) {
                    int step = in.read(data, read, data.length - read);
                    if (step < 0) {
                        break;
                    }
                    read += step;
                }
            }
            String trace = new String(data, "UTF-8");
            return trace.length() > MAX ? trace.substring(0, MAX) : trace;
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static void showCrashScreen(Context app, Throwable error) {
        Intent intent = CrashActivity.intent(app, traceOf(error));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        app.startActivity(intent);
        // Give the system a moment to start the crash process before the default
        // handler kills this one. The crash screen is in a separate process.
        try {
            Thread.sleep(700);
        } catch (InterruptedException ignored) {
        }
    }
}
