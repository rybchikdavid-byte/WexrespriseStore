package com.wexresprise.store;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int REQ_PICK = 101;
    private static final int REQ_STORAGE = 102;

    private Prefs prefs;
    private StoreApi api;
    private final Handler main = new Handler(Looper.getMainLooper());

    private FrameLayout root;
    private LinearLayout content;
    private LinearLayout tabsBox;
    private TextView tabAll;
    private TextView tabProjects;
    private TextView subtitleView;
    private LinearLayout demoBanner;
    private FrameLayout listHost;
    private ListView list;
    private StoreAdapter adapter;
    private LinearLayout stateBox;
    private ProgressBar stateProgress;
    private ImageView stateIcon;
    private TextView stateTitle;
    private TextView stateSub;
    private TextView stateAction;
    private LinearLayout publishButton;

    private int accent;
    private int onAccent;
    private int textColor;
    private int subColor;
    private int bottomInset;
    private int tab;
    private boolean loading;
    private String error;
    private List<StoreItem> items = new ArrayList<>();
    private StoreItem pendingDownload;
    private String lastConfig = "";

    private Uri pickUri;
    private String pickName = "";
    private long pickSize;

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
        updateTabs();
        updateState();
        refresh();
        lastConfig = configSignature();
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
        GlassDrawable d = new GlassDrawable(dp(radius), fill, stroke, Math.max(1f, getResources()
                .getDisplayMetrics().density));
        d.setGloss(gloss);
        return d;
    }

    private void buildUi() {
        root = new FrameLayout(this);

        AuroraView aurora = new AuroraView(this);
        aurora.setAccent(accent);
        root.addView(aurora, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(16), dp(18), 0);
        root.addView(content, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        content.addView(buildHeader(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        demoBanner = buildDemoBanner();
        LinearLayout.LayoutParams bannerLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bannerLp.topMargin = dp(12);
        content.addView(demoBanner, bannerLp);

        tabsBox = buildTabs();
        LinearLayout.LayoutParams tabsLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        tabsLp.topMargin = dp(14);
        content.addView(tabsBox, tabsLp);

        listHost = new FrameLayout(this);
        LinearLayout.LayoutParams hostLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        hostLp.topMargin = dp(12);
        content.addView(listHost, hostLp);

        list = new ListView(this);
        list.setDivider(new ColorDrawable(Color.TRANSPARENT));
        list.setDividerHeight(dp(10));
        list.setSelector(new ColorDrawable(Color.TRANSPARENT));
        list.setVerticalScrollBarEnabled(false);
        list.setClipToPadding(false);
        list.setPadding(0, 0, 0, dp(96));
        listHost.addView(list, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        adapter = new StoreAdapter(this, new StoreAdapter.Listener() {
            @Override
            public void onDownload(StoreItem item) {
                download(item);
            }

            @Override
            public void onDetails(StoreItem item) {
                showDetails(item);
            }
        }, accent);
        list.setAdapter(adapter);

        stateBox = buildState();
        listHost.addView(stateBox, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER));

        publishButton = buildPublishButton();
        FrameLayout.LayoutParams fabLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(50), Gravity.BOTTOM | Gravity.END);
        fabLp.rightMargin = dp(18);
        fabLp.bottomMargin = dp(20);
        root.addView(publishButton, fabLp);

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
            bottomInset = bottom;
            content.setPadding(dp(18), top + dp(14), dp(18), 0);
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) publishButton.getLayoutParams();
            lp.bottomMargin = bottom + dp(18);
            publishButton.setLayoutParams(lp);
            list.setPadding(0, 0, 0, bottom + dp(92));
            return insets;
        });

        setContentView(root);
    }

    private View buildHeader() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.ic_store);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logo.setPadding(dp(9), dp(9), dp(9), dp(9));
        logo.setBackground(glass(16, BadgeView.withAlpha(accent, 16),
                BadgeView.withAlpha(accent, 38), true));
        row.addView(logo, new LinearLayout.LayoutParams(dp(46), dp(46)));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams titlesLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        titlesLp.leftMargin = dp(12);
        row.addView(titles, titlesLp);

        TextView name = label(getString(R.string.app_name), 18, textColor, true);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        titles.addView(name);

        subtitleView = label(getString(R.string.subtitle), 11.5f, subColor, false);
        subtitleView.setSingleLine(true);
        subtitleView.setEllipsize(TextUtils.TruncateAt.END);
        titles.addView(subtitleView);

        ImageView refresh = iconButton(R.drawable.ic_refresh, R.string.refresh);
        refresh.setOnClickListener(v -> refresh());
        row.addView(refresh, new LinearLayout.LayoutParams(dp(40), dp(40)));

        ImageView settings = iconButton(R.drawable.ic_settings, R.string.settings_title);
        settings.setOnClickListener(v -> openSettings());
        LinearLayout.LayoutParams settingsLp = new LinearLayout.LayoutParams(dp(40), dp(40));
        settingsLp.leftMargin = dp(6);
        row.addView(settings, settingsLp);

        return row;
    }

    private ImageView iconButton(int res, int description) {
        ImageView v = new ImageView(this);
        v.setImageResource(res);
        v.setContentDescription(getString(description));
        v.setColorFilter(subColor);
        v.setPadding(dp(10), dp(10), dp(10), dp(10));
        v.setBackground(glass(20, 0x14FFFFFF, 0x2AFFFFFF, false));
        return v;
    }

    private LinearLayout buildDemoBanner() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(14), dp(10), dp(14), dp(10));
        bar.setBackground(glass(20, BadgeView.withAlpha(accent, 12),
                BadgeView.withAlpha(accent, 34), false));

        View dot = new View(this);
        dot.setBackground(glass(4, accent, 0, false));
        bar.addView(dot, new LinearLayout.LayoutParams(dp(8), dp(8)));

        TextView text = label(getString(R.string.demo_banner), 12, accent, true);
        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textLp.leftMargin = dp(9);
        bar.addView(text, textLp);

        TextView action = label(getString(R.string.setup), 12, accent, true);
        action.setPadding(dp(8), dp(2), 0, dp(2));
        bar.addView(action);

        bar.setOnClickListener(v -> openSettings());
        return bar;
    }

    private LinearLayout buildTabs() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setPadding(dp(4), dp(4), dp(4), dp(4));
        box.setBackground(glass(23, 0x12FFFFFF, 0x26FFFFFF, true));

        tabAll = tabLabel(getString(R.string.tab_all));
        tabAll.setOnClickListener(v -> selectTab(0));
        box.addView(tabAll, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        tabProjects = tabLabel(getString(R.string.tab_projects));
        tabProjects.setOnClickListener(v -> selectTab(1));
        LinearLayout.LayoutParams projectLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1.25f);
        projectLp.leftMargin = dp(4);
        box.addView(tabProjects, projectLp);

        return box;
    }

    private TextView tabLabel(String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setGravity(Gravity.CENTER);
        v.setSingleLine(true);
        v.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        v.setAutoSizeTextTypeUniformWithConfiguration(9, 13, 1,
                TypedValue.COMPLEX_UNIT_SP);
        return v;
    }

    private LinearLayout buildState() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setPadding(dp(24), dp(12), dp(24), dp(80));

        stateProgress = new ProgressBar(this);
        stateProgress.setIndeterminateTintList(ColorStateList.valueOf(accent));
        LinearLayout.LayoutParams progressLp = new LinearLayout.LayoutParams(
                dp(30), dp(30));
        box.addView(stateProgress, progressLp);

        stateIcon = new ImageView(this);
        stateIcon.setImageResource(R.drawable.ic_store);
        stateIcon.setAlpha(0.45f);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(72), dp(72));
        box.addView(stateIcon, iconLp);

        stateTitle = label("", 16, textColor, true);
        stateTitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleLp.topMargin = dp(10);
        box.addView(stateTitle, titleLp);

        stateSub = label("", 12.5f, subColor, false);
        stateSub.setGravity(Gravity.CENTER);
        stateSub.setMaxWidth(dp(280));
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = dp(6);
        box.addView(stateSub, subLp);

        stateAction = label("", 13, onAccent, true);
        stateAction.setGravity(Gravity.CENTER);
        stateAction.setPadding(dp(18), dp(10), dp(18), dp(10));
        stateAction.setBackground(glass(20, accent, 0x33FFFFFF, false));
        LinearLayout.LayoutParams actionLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        actionLp.topMargin = dp(14);
        box.addView(stateAction, actionLp);

        stateAction.setOnClickListener(v -> {
            if (error != null) {
                refresh();
            } else {
                startPublish();
            }
        });

        return box;
    }

    private LinearLayout buildPublishButton() {
        LinearLayout button = new LinearLayout(this);
        button.setOrientation(LinearLayout.HORIZONTAL);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(18), 0, dp(20), 0);
        button.setBackground(glass(25, accent, 0x40FFFFFF, true));
        button.setElevation(dp(10));

        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_upload);
        icon.setColorFilter(onAccent);
        button.addView(icon, new LinearLayout.LayoutParams(dp(18), dp(18)));

        TextView text = label(getString(R.string.publish), 14, onAccent, true);
        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        textLp.leftMargin = dp(8);
        button.addView(text, textLp);

        button.setOnClickListener(v -> startPublish());
        return button;
    }

    private void updateTabs() {
        for (int i = 0; i < 2; i++) {
            TextView tabView = i == 0 ? tabAll : tabProjects;
            boolean selected = tab == i;
            tabView.setTextColor(selected ? onAccent : subColor);
            tabView.setBackground(selected ? glass(19, accent, 0, false) : null);
        }
    }

    private void selectTab(int index) {
        if (tab == index) {
            return;
        }
        tab = index;
        updateTabs();
        adapter.setItems(filtered());
        updateState();
    }

    private List<StoreItem> filtered() {
        if (tab == 0) {
            return items;
        }
        List<StoreItem> out = new ArrayList<>();
        for (StoreItem item : items) {
            if (item.belongsToProjects()) {
                out.add(item);
            }
        }
        return out;
    }

    private void updateState() {
        boolean demo = !prefs.configured();
        demoBanner.setVisibility(demo ? View.VISIBLE : View.GONE);

        boolean loaded = !loading && error == null;
        List<StoreItem> visible = filtered();
        subtitleView.setText(loaded
                ? getString(R.string.count_label, visible.size())
                : getString(R.string.subtitle));

        boolean hasItems = loaded && !visible.isEmpty();
        list.setVisibility(hasItems ? View.VISIBLE : View.INVISIBLE);

        boolean showState = !hasItems;
        stateBox.setVisibility(showState ? View.VISIBLE : View.GONE);
        if (!showState) {
            return;
        }

        stateProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        stateIcon.setVisibility(loading ? View.GONE : View.VISIBLE);
        stateSub.setVisibility(loading ? View.GONE : View.VISIBLE);
        stateAction.setVisibility(loading ? View.GONE : View.VISIBLE);

        if (loading) {
            stateTitle.setText(R.string.loading);
            stateSub.setText("");
        } else if (error != null) {
            stateTitle.setText(R.string.load_error);
            stateSub.setText(error);
            stateAction.setText(R.string.retry);
        } else if (tab == 1) {
            stateTitle.setText(R.string.empty_projects);
            stateSub.setText(R.string.empty_hint);
            stateAction.setText(R.string.publish);
        } else {
            stateTitle.setText(R.string.empty_all);
            stateSub.setText(R.string.empty_hint);
            stateAction.setText(R.string.publish);
        }
    }

    private void refresh() {
        if (loading) {
            return;
        }
        loading = true;
        error = null;
        updateState();
        api.load(prefs, new StoreApi.ListCallback() {
            @Override
            public void onSuccess(List<StoreItem> loaded) {
                loading = false;
                error = null;
                items = loaded;
                adapter.setItems(filtered());
                updateState();
            }

            @Override
            public void onError(String message) {
                loading = false;
                error = message;
                items = new ArrayList<>();
                adapter.setItems(filtered());
                updateState();
            }
        });
    }

    private String configSignature() {
        return prefs.url() + "|" + prefs.key();
    }

    @Override
    protected void onResume() {
        super.onResume();
        String now = configSignature();
        if (!now.equals(lastConfig)) {
            lastConfig = now;
            refresh();
        }
    }

    private void openSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    // ------------------------------------------------------------- publish

    private void startPublish() {
        if (!prefs.configured()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.server_required)
                    .setMessage(R.string.server_required_text)
                    .setPositiveButton(R.string.open_settings, (d, w) -> openSettings())
                    .setNegativeButton(R.string.cancel, null)
                    .show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivityForResult(intent, REQ_PICK);
        } catch (Exception e) {
            toast(getString(R.string.no_file));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_PICK || resultCode != RESULT_OK || data == null
                || data.getData() == null) {
            return;
        }
        Uri uri = data.getData();
        pickUri = uri;
        pickName = queryName(uri);
        pickSize = querySize(uri);
        try {
            getContentResolver().takePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Throwable ignored) {
        }
        showPublishDialog();
    }

    private String queryName(Uri uri) {
        try (android.database.Cursor c = getContentResolver().query(uri,
                new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                String name = c.getString(0);
                if (name != null && !name.isEmpty()) {
                    return name;
                }
            }
        } catch (Throwable ignored) {
        }
        String last = uri.getLastPathSegment();
        return last == null ? "file" : last;
    }

    private long querySize(Uri uri) {
        try (android.database.Cursor c = getContentResolver().query(uri,
                new String[]{OpenableColumns.SIZE}, null, null, null)) {
            if (c != null && c.moveToFirst() && !c.isNull(0)) {
                return Math.max(0, c.getLong(0));
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }

    private void showPublishDialog() {
        String base = pickName;
        int dot = base.lastIndexOf('.');
        if (dot > 0) {
            base = base.substring(0, dot);
        }

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(22), dp(8), dp(22), dp(4));

        EditText title = new EditText(this);
        title.setHint(R.string.name_hint);
        title.setSingleLine(true);
        title.setText(base);
        title.setSelectAllOnFocus(true);
        box.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        EditText description = new EditText(this);
        description.setHint(R.string.desc_hint);
        description.setMinLines(2);
        description.setMaxLines(4);
        description.setGravity(Gravity.TOP | Gravity.START);
        LinearLayout.LayoutParams descLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        descLp.topMargin = dp(6);
        box.addView(description, descLp);

        String info = pickName + (pickSize > 0 ? "  ·  " + Formats.size(pickSize) : "");
        TextView fileInfo = label(info, 11.5f, subColor, false);
        fileInfo.setSingleLine(true);
        fileInfo.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        infoLp.topMargin = dp(4);
        box.addView(fileInfo, infoLp);

        CheckBox wex = new CheckBox(this);
        wex.setText(R.string.wex_switch);
        wex.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        wex.setTextColor(textColor);
        wex.setChecked(isProjectName(pickName));
        LinearLayout.LayoutParams wexLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        wexLp.topMargin = dp(8);
        box.addView(wex, wexLp);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.publish_title)
                .setView(box)
                .setPositiveButton(R.string.publish, null)
                .setNegativeButton(R.string.cancel, null)
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = title.getText().toString().trim();
            if (name.isEmpty()) {
                title.setError(getString(R.string.name_required));
                return;
            }
            String desc = description.getText().toString().trim();
            dialog.dismiss();
            upload(name, desc, wex.isChecked());
        });
    }

    private boolean isProjectName(String name) {
        String n = name == null ? "" : name.toLowerCase(Locale.ROOT);
        return n.contains("wexresprise") || n.contains("pixelgram");
    }

    private void upload(String title, String description, boolean isWex) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(22), dp(18), dp(22), dp(6));

        TextView status = label(getString(R.string.uploading), 14, textColor, true);
        status.setSingleLine(true);
        status.setEllipsize(TextUtils.TruncateAt.END);
        box.addView(status);

        ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        bar.setProgress(0);
        bar.setProgressTintList(ColorStateList.valueOf(accent));
        bar.setProgressBackgroundTintList(ColorStateList.valueOf(0x33FFFFFF));
        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(6));
        barLp.topMargin = dp(16);
        box.addView(bar, barLp);

        TextView percent = label("0%", 12, subColor, false);
        LinearLayout.LayoutParams percentLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        percentLp.topMargin = dp(8);
        percentLp.gravity = Gravity.END;
        box.addView(percent, percentLp);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(box)
                .setCancelable(false)
                .create();
        dialog.show();

        api.upload(prefs, pickUri, title, description, isWex, new StoreApi.UploadCallback() {
            @Override
            public void onProgress(int value) {
                bar.setProgress(value);
                percent.setText(value + "%");
            }

            @Override
            public void onSuccess() {
                if (!isFinishing()) {
                    dialog.dismiss();
                }
                toast(getString(R.string.published));
                refresh();
            }

            @Override
            public void onError(String message) {
                if (!isFinishing()) {
                    dialog.dismiss();
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle(R.string.upload_failed)
                            .setMessage(message)
                            .setPositiveButton(R.string.close, null)
                            .show();
                }
            }
        });
    }

    // ------------------------------------------------------------ download

    private void download(StoreItem item) {
        if (TextUtils.isEmpty(item.downloadUrl)) {
            toast(getString(R.string.download_failed));
            return;
        }
        if (Build.VERSION.SDK_INT <= 28
                && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            pendingDownload = item;
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQ_STORAGE);
            return;
        }
        enqueueDownload(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQ_STORAGE) {
            return;
        }
        boolean granted = grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        if (granted && pendingDownload != null) {
            enqueueDownload(pendingDownload);
        } else {
            toast(getString(R.string.permission_denied));
        }
        pendingDownload = null;
    }

    private void enqueueDownload(StoreItem item) {
        try {
            DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            DownloadManager.Request request = new DownloadManager.Request(
                    Uri.parse(item.downloadUrl));
            request.setTitle(item.title);
            if (!TextUtils.isEmpty(item.description)) {
                request.setDescription(item.description);
            }
            request.setMimeType(item.mime);
            request.setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
                    fileName(item));
            request.allowScanningByMediaScanner();
            dm.enqueue(request);
            toast(getString(R.string.download_started));
        } catch (Exception e) {
            toast(getString(R.string.download_failed));
        }
    }

    private String fileName(StoreItem item) {
        String base = item.title.isEmpty() ? "file" : item.title;
        base = base.replaceAll("[\\\\/:*?\"<>|]+", "-").trim();
        if (base.isEmpty()) {
            base = "file";
        }
        if (base.length() > 80) {
            base = base.substring(0, 80);
        }
        String ext = item.extension();
        if (ext.isEmpty()) {
            ext = Formats.extensionForMime(item.mime);
        }
        if (!ext.isEmpty() && !base.toLowerCase(Locale.ROOT).endsWith("." + ext)) {
            base = base + "." + ext;
        }
        return base;
    }

    // ------------------------------------------------------------- details

    private void showDetails(StoreItem item) {
        StringBuilder body = new StringBuilder();
        if (!TextUtils.isEmpty(item.description)) {
            body.append(item.description).append("\n\n");
        }
        body.append(Formats.size(item.size));
        String date = Formats.date(item.createdAt);
        if (!date.isEmpty()) {
            body.append("  ·  ").append(date);
        }
        if (!TextUtils.isEmpty(item.mime)) {
            body.append("\n").append(item.mime);
        }
        if (item.belongsToProjects()) {
            body.append("\n\nWexresprise Projects");
        }

        new AlertDialog.Builder(this)
                .setTitle(item.title)
                .setMessage(body.toString())
                .setPositiveButton(R.string.download, (d, w) -> download(item))
                .setNegativeButton(R.string.close, null)
                .show();
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        main.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
