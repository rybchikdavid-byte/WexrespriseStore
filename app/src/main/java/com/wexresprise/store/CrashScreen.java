package com.wexresprise.store;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * A crash report made only from plain widgets and colours: it must work when the theme system,
 * the .ws style or a preference is exactly what broke.
 */
public final class CrashScreen {

    private static final int BG = 0xFF0B0D12;
    private static final int CARD = 0xFF141820;
    private static final int TEXT = 0xFFE7EAF0;
    private static final int SUB = 0xFF9BA5B4;
    private static final int ACCENT = 0xFFA8C7FA;
    private static final int ON_ACCENT = 0xFF0A1B33;
    private static final int DANGER = 0xFFFFB4AB;

    private CrashScreen() {
    }

    public static View build(final Activity activity, String trace, final Runnable onReset) {
        String full = header(activity) + (trace == null ? "" : trace);

        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(activity, 18);
        content.setPadding(pad, pad, pad, pad);
        scroll.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = text(activity, activity.getString(R.string.crash_title), 20, DANGER, true);
        content.addView(title);

        TextView hint = text(activity, activity.getString(R.string.crash_hint), 12.5f, SUB, false);
        hint.setLineSpacing(dp(activity, 2), 1f);
        LinearLayout.LayoutParams hintLp = params(activity, 6);
        content.addView(hint, hintLp);

        TextView body = text(activity, full, 10.5f, TEXT, false);
        body.setTypeface(Typeface.MONOSPACE);
        body.setTextIsSelectable(true);
        body.setPadding(dp(activity, 12), dp(activity, 12), dp(activity, 12), dp(activity, 12));
        GradientDrawable card = new GradientDrawable();
        card.setColor(CARD);
        card.setCornerRadius(dp(activity, 12));
        card.setStroke(Math.max(1, dp(activity, 1)), 0x33FFFFFF);
        body.setBackground(card);
        content.addView(body, params(activity, 14));

        content.addView(button(activity, activity.getString(R.string.crash_copy),
                ACCENT, ON_ACCENT, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        copy(activity, full);
                    }
                }));
        content.addView(button(activity, activity.getString(R.string.crash_share),
                0x14FFFFFF, TEXT, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        share(activity, full);
                    }
                }));
        if (onReset != null) {
            content.addView(button(activity, activity.getString(R.string.crash_reset),
                    0x1AFFB4AB, DANGER, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            onReset.run();
                        }
                    }));
        }
        content.addView(button(activity, activity.getString(R.string.crash_close),
                0x14FFFFFF, TEXT, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        activity.finish();
                    }
                }));

        return scroll;
    }

    private static String header(Activity activity) {
        String version = "";
        try {
            PackageInfo info = activity.getPackageManager()
                    .getPackageInfo(activity.getPackageName(), 0);
            long code = Build.VERSION.SDK_INT >= 28
                    ? info.getLongVersionCode() : info.versionCode;
            version = info.versionName + " (" + code + ")";
        } catch (Throwable ignored) {
        }
        return "Wexresprise Store " + version
                + "\nAndroid " + Build.VERSION.RELEASE + " (SDK " + Build.VERSION.SDK_INT + ")"
                + "\n" + Build.MANUFACTURER + " " + Build.MODEL
                + "\n\n";
    }

    private static TextView text(Activity activity, CharSequence value, float sizeSp, int color,
                                 boolean medium) {
        TextView view = new TextView(activity);
        view.setText(value);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        view.setTextColor(color);
        if (medium) {
            view.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        }
        return view;
    }

    private static TextView button(Activity activity, String label, int fill, int color,
                                   View.OnClickListener click) {
        TextView view = new TextView(activity);
        view.setText(label);
        view.setGravity(Gravity.CENTER);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        view.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        view.setTextColor(color);
        view.setPadding(dp(activity, 14), 0, dp(activity, 14), 0);
        GradientDrawable background = new GradientDrawable();
        background.setColor(fill);
        background.setCornerRadius(dp(activity, 20));
        view.setBackground(background);
        view.setOnClickListener(click);
        LinearLayout.LayoutParams lp = params(activity, 10);
        lp.height = dp(activity, 46);
        view.setLayoutParams(lp);
        return view;
    }

    private static LinearLayout.LayoutParams params(Activity activity, int topDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(activity, topDp);
        return lp;
    }

    private static void copy(Activity activity, String value) {
        try {
            ClipboardManager manager = (ClipboardManager) activity.getSystemService(
                    Context.CLIPBOARD_SERVICE);
            if (manager != null) {
                manager.setPrimaryClip(ClipData.newPlainText("Wexresprise crash", value));
            }
            Toast.makeText(activity, R.string.crash_copied, Toast.LENGTH_SHORT).show();
        } catch (Throwable ignored) {
        }
    }

    private static void share(Activity activity, String value) {
        try {
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("text/plain");
            send.putExtra(Intent.EXTRA_SUBJECT, activity.getString(R.string.crash_title));
            send.putExtra(Intent.EXTRA_TEXT, value);
            activity.startActivity(Intent.createChooser(send,
                    activity.getString(R.string.crash_share)));
        } catch (Throwable ignored) {
            copy(activity, value);
        }
    }

    private static int dp(Activity activity, float value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
