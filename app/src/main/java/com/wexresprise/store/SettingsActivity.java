package com.wexresprise.store;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class SettingsActivity extends Activity {

    private Prefs prefs;
    private StoreApi api;
    private int accent;
    private int onAccent;
    private int textColor;
    private int subColor;

    private EditText urlField;
    private EditText keyField;
    private TextView statusView;
    private TextView testButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new Prefs(this);
        api = new StoreApi(this);
        textColor = getColor(R.color.wx_text);
        subColor = getColor(R.color.wx_sub);
        accent = resolveAccent();
        onAccent = Formats.contrast(accent);

        edgeToEdge();
        buildUi();
    }

    private int resolveAccent() {
        if (Build.VERSION.SDK_INT >= 31) {
            try {
                int c = getColor(android.R.color.system_accent1_200);
                if (Color.alpha(c) == 255) {
                    return c;
                }
            } catch (Throwable ignored) {
            }
        }
        return getColor(R.color.wx_primary);
    }

    private void edgeToEdge() {
        Window w = getWindow();
        w.setStatusBarColor(Color.TRANSPARENT);
        w.setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= 30) {
            w.setDecorFitsSystemWindows(false);
        } else {
            w.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    }

    private int dp(float value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                getResources().getDisplayMetrics()));
    }

    private TextView label(CharSequence text, float sp, int color, boolean medium) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        v.setTextColor(color);
        if (medium) {
            v.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        }
        return v;
    }

    private GlassDrawable glass(float radius, int fill, int stroke, boolean gloss) {
        GlassDrawable d = new GlassDrawable(dp(radius), fill, stroke, Math.max(1f,
                getResources().getDisplayMetrics().density));
        d.setGloss(gloss);
        return d;
    }

    private void buildUi() {
        FrameLayout root = new FrameLayout(this);

        AuroraView aurora = new AuroraView(this);
        aurora.setAccent(accent);
        root.addView(aurora, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        root.addView(scroll, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(14), dp(18), dp(28));
        scroll.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        content.addView(buildHeader());

        LinearLayout server = section(getString(R.string.server));
        content.addView(server, cardLp());

        TextView serverHint = label(getString(R.string.server_hint), 11.5f, subColor, false);
        server.addView(serverHint);

        urlField = field(getString(R.string.server_url), InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_URI);
        urlField.setText(prefs.url());
        LinearLayout.LayoutParams urlLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        urlLp.topMargin = dp(10);
        server.addView(urlField, urlLp);

        keyField = field(getString(R.string.server_key), InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        keyField.setText(prefs.key());
        keyField.setTypeface(Typeface.MONOSPACE);
        keyField.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        LinearLayout.LayoutParams keyLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        keyLp.topMargin = dp(8);
        server.addView(keyField, keyLp);

        statusView = label("", 12, subColor, false);
        statusView.setVisibility(View.GONE);
        LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        statusLp.topMargin = dp(10);
        server.addView(statusView, statusLp);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams actionsLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        actionsLp.topMargin = dp(14);
        server.addView(actions, actionsLp);

        TextView save = pill(getString(R.string.save), true);
        save.setOnClickListener(v -> save());
        LinearLayout.LayoutParams saveLp = new LinearLayout.LayoutParams(
                0, dp(44), 1f);
        actions.addView(save, saveLp);

        testButton = pill(getString(R.string.test), false);
        testButton.setOnClickListener(v -> test());
        LinearLayout.LayoutParams testLp = new LinearLayout.LayoutParams(
                0, dp(44), 1f);
        testLp.leftMargin = dp(10);
        actions.addView(testButton, testLp);

        TextView reset = label(getString(R.string.reset_server), 12.5f, 0xFFFFB4AB, true);
        reset.setGravity(Gravity.CENTER);
        reset.setPadding(dp(14), dp(10), dp(14), dp(10));
        LinearLayout.LayoutParams resetLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        resetLp.topMargin = dp(8);
        server.addView(reset, resetLp);
        reset.setOnClickListener(v -> confirmReset());

        LinearLayout setup = section(getString(R.string.setup));
        content.addView(setup, cardLp());

        TextView help = label(getString(R.string.setup_help), 12.5f, subColor, false);
        help.setLineSpacing(dp(3), 1f);
        setup.addView(help);

        TextView sqlHint = label(getString(R.string.sql_button_hint), 11.5f, subColor, false);
        LinearLayout.LayoutParams hintLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hintLp.topMargin = dp(14);
        setup.addView(sqlHint, hintLp);

        TextView copy = pill(getString(R.string.copy_sql), false);
        copy.setOnClickListener(v -> copySql());
        LinearLayout.LayoutParams copyLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
        copyLp.topMargin = dp(10);
        setup.addView(copy, copyLp);

        TextView open = pill(getString(R.string.open_supabase), false);
        open.setOnClickListener(v -> openSupabase());
        LinearLayout.LayoutParams openLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
        openLp.topMargin = dp(8);
        setup.addView(open, openLp);

        LinearLayout about = section(getString(R.string.about));
        content.addView(about, cardLp());

        TextView aboutText = label(getString(R.string.about_text), 12.5f, subColor, false);
        aboutText.setLineSpacing(dp(3), 1f);
        about.addView(aboutText);

        TextView version = label(getString(R.string.version_label, BuildConfig.VERSION_NAME),
                11.5f, subColor, false);
        LinearLayout.LayoutParams versionLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        versionLp.topMargin = dp(16);
        versionLp.gravity = Gravity.CENTER_HORIZONTAL;
        version.setGravity(Gravity.CENTER);
        content.addView(version, versionLp);

        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int top;
            int bottom;
            if (Build.VERSION.SDK_INT >= 30) {
                Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                top = bars.top;
                bottom = bars.bottom;
            } else {
                top = insets.getSystemWindowInsetTop();
                bottom = insets.getSystemWindowInsetBottom();
            }
            content.setPadding(dp(18), top + dp(12), dp(18), bottom + dp(28));
            return insets;
        });

        setContentView(root);
    }

    private View buildHeader() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        ImageView back = new ImageView(this);
        back.setImageResource(R.drawable.ic_back);
        back.setColorFilter(textColor);
        back.setContentDescription(getString(R.string.close));
        back.setPadding(dp(10), dp(10), dp(10), dp(10));
        back.setBackground(glass(20, 0x14FFFFFF, 0x2AFFFFFF, false));
        back.setOnClickListener(v -> finish());
        row.addView(back, new LinearLayout.LayoutParams(dp(40), dp(40)));

        TextView title = label(getString(R.string.settings_title), 21, textColor, true);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        titleLp.leftMargin = dp(12);
        row.addView(title, titleLp);

        return row;
    }

    private LinearLayout section(String title) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(14));
        GlassDrawable bg = glass(22, 0x14FFFFFF, 0x2AFFFFFF, true);
        bg.setGlossAlpha(0x2A);
        card.setBackground(bg);

        TextView heading = label(title, 14, textColor, true);
        LinearLayout.LayoutParams headingLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        headingLp.bottomMargin = dp(8);
        card.addView(heading, headingLp);
        return card;
    }

    private LinearLayout.LayoutParams cardLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(14);
        return lp;
    }

    private EditText field(String hint, int inputType) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setHintTextColor(0x809BA5B4);
        e.setTextColor(textColor);
        e.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        e.setSingleLine(true);
        e.setInputType(inputType);
        e.setBackgroundTintList(ColorStateList.valueOf(BadgeView.withAlpha(accent, 55)));
        e.setPadding(0, dp(6), 0, dp(8));
        return e;
    }

    private TextView pill(String text, boolean primary) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setGravity(Gravity.CENTER);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        v.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        if (primary) {
            v.setTextColor(onAccent);
            v.setBackground(glass(22, accent, 0x30FFFFFF, false));
        } else {
            v.setTextColor(accent);
            v.setBackground(glass(22, BadgeView.withAlpha(accent, 14),
                    BadgeView.withAlpha(accent, 42), false));
        }
        return v;
    }

    private void save() {
        String url = urlField.getText().toString().trim();
        String key = keyField.getText().toString().trim();
        if (!url.isEmpty() && !url.startsWith("http://") && !url.startsWith("https://")) {
            urlField.setError(getString(R.string.invalid_url));
            return;
        }
        if (url.isEmpty() && key.isEmpty()) {
            prefs.clearServer();
        } else if (url.isEmpty() || key.isEmpty()) {
            showStatus(getString(R.string.fields_required), false);
            return;
        } else {
            prefs.setServer(url, key);
            urlField.setText(prefs.url());
        }
        toast(getString(R.string.saved));
        showStatus(getString(R.string.saved), true);
    }

    private void test() {
        String url = urlField.getText().toString().trim();
        String key = keyField.getText().toString().trim();
        if (url.isEmpty() || key.isEmpty()) {
            showStatus(getString(R.string.fields_required), false);
            return;
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            urlField.setError(getString(R.string.invalid_url));
            return;
        }
        testButton.setEnabled(false);
        testButton.setText(R.string.testing);
        showStatus(getString(R.string.testing), true);
        api.ping(url, key, new StoreApi.CountCallback() {
            @Override
            public void onSuccess(int count) {
                testButton.setEnabled(true);
                testButton.setText(R.string.test);
                showStatus(getString(R.string.connected, count), true);
            }

            @Override
            public void onError(String message) {
                testButton.setEnabled(true);
                testButton.setText(R.string.test);
                showStatus(getString(R.string.connect_failed, message), false);
            }
        });
    }

    private void showStatus(String text, boolean ok) {
        statusView.setVisibility(View.VISIBLE);
        statusView.setText(text);
        statusView.setTextColor(ok ? 0xFF7BD88F : 0xFFFFB4AB);
    }

    private void confirmReset() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.reset_confirm)
                .setPositiveButton(R.string.reset_server, (d, w) -> {
                    prefs.clearServer();
                    urlField.setText("");
                    keyField.setText("");
                    statusView.setVisibility(View.GONE);
                    toast(getString(R.string.reset_done));
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void copySql() {
        try {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("setup.sql", readSql()));
            toast(getString(R.string.sql_copied));
        } catch (Throwable e) {
            toast(getString(R.string.connect_failed, String.valueOf(e.getMessage())));
        }
    }

    private String readSql() throws Exception {
        try (InputStream in = getResources().openRawResource(R.raw.setup);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toString("UTF-8");
        }
    }

    private void openSupabase() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://supabase.com/dashboard/projects"));
            startActivity(intent);
        } catch (Throwable ignored) {
            toast(getString(R.string.open_supabase));
        }
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
}
