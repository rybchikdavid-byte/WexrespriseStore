package com.wexresprise.store;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.wexresprise.store.style.InstalledStyle;
import com.wexresprise.store.style.StyleConfig;
import com.wexresprise.store.style.StyleManager;

import java.io.File;
import java.util.List;

/**
 * All appearance settings plus the .ws style shelf. Every change is stored in {@link Prefs} and
 * the activity recreates itself so the new palette is visible immediately.
 */
public class AppearanceActivity extends BaseStoreActivity {

    private static final int REQ_STYLE = 201;

    private interface Pick {
        void on(int index);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
    }

    private void buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.addView(buildBackground(), new FrameLayout.LayoutParams(
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

        content.addView(themeSection(), cardLp());
        content.addView(accentSection(), cardLp());
        content.addView(launcherSection(), cardLp());
        content.addView(backgroundSection(), cardLp());
        content.addView(cardsSection(), cardLp());
        content.addView(shapeSection(), cardLp());
        content.addView(densitySection(), cardLp());
        content.addView(stylesSection(), cardLp());
        content.addView(resetSection(), cardLp());

        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int top = screenTopInset(insets);
            int bottom = screenBottomInset(insets);
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
        back.setBackground(glass(20, style.night ? 0x14FFFFFF : 0x0F000000,
                style.night ? 0x2AFFFFFF : 0x1A000000, false));
        back.setOnClickListener(v -> finish());
        row.addView(back, new LinearLayout.LayoutParams(dp(40), dp(40)));

        TextView title = label(getString(R.string.appearance_title), 21, textColor, true);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        titleLp.leftMargin = dp(12);
        row.addView(title, titleLp);

        return row;
    }

    // ------------------------------------------------------------------ cards

    private LinearLayout section(String title, String hint) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(14));
        GlassDrawable bg = cardBackground();
        if (style.cardStyle == StyleConfig.CARDS_GLASS) {
            bg.setGlossAlpha(style.night ? 0x2A : 0x60);
        }
        card.setBackground(bg);

        TextView heading = label(title, 14, textColor, true);
        card.addView(heading);

        if (!TextUtils.isEmpty(hint)) {
            TextView sub = label(hint, 11.5f, subColor, false);
            sub.setLineSpacing(dp(2), 1f);
            LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            subLp.topMargin = dp(4);
            card.addView(sub, subLp);
        }
        return card;
    }

    private LinearLayout.LayoutParams cardLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(14);
        return lp;
    }

    private HorizontalScrollView chips(String[] labels, int selected, Pick pick) {
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(10), 0, 0);
        scroll.addView(row, new HorizontalScrollView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            boolean active = i == selected;
            TextView chip = label(labels[i], 12.5f, active ? onAccent : textColor, true);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(14), dp(9), dp(14), dp(9));
            chip.setBackground(active
                    ? glass(20, accent, 0, false)
                    : glass(20, style.night ? 0x10FFFFFF : 0x0A000000,
                    style.night ? 0x22FFFFFF : 0x18000000, false));
            chip.setOnClickListener(v -> pick.on(index));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = dp(8);
            row.addView(chip, lp);
        }
        return scroll;
    }

    private LinearLayout themeSection() {
        LinearLayout card = section(getString(R.string.appearance_theme),
                getString(R.string.appearance_theme_hint));
        card.addView(chips(new String[]{
                getString(R.string.theme_system),
                getString(R.string.theme_light),
                getString(R.string.theme_dark)
        }, prefs.theme(), index -> {
            prefs.setTheme(index);
            recreate();
        }));
        return card;
    }

    private LinearLayout accentSection() {
        LinearLayout card = section(getString(R.string.appearance_accent),
                getString(R.string.appearance_accent_hint));

        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(12), 0, 0);
        scroll.addView(row, new HorizontalScrollView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        int chosen = prefs.accent();
        for (int i = 0; i < StoreTheme.ACCENTS.length; i++) {
            final int value = StoreTheme.ACCENTS[i];
            boolean active = value == chosen;
            View swatch = new View(this);
            int color = value == 0 ? StoreTheme.displayAccent(this, prefs) : value;
            swatch.setBackground(glass(17, color,
                    active ? (style.night ? 0xFFFFFFFF : 0xFF151A22)
                            : (style.night ? 0x33FFFFFF : 0x22000000),
                    false));
            swatch.setOnClickListener(v -> {
                prefs.setAccent(value);
                recreate();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(34), dp(34));
            lp.rightMargin = dp(10);
            row.addView(swatch, lp);
        }
        card.addView(scroll);
        return card;
    }

    private LinearLayout launcherSection() {
        LinearLayout card = section(getString(R.string.appearance_icon),
                getString(R.string.appearance_icon_hint));

        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(12), 0, 0);
        scroll.addView(row, new HorizontalScrollView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        int[] icons = {
                R.mipmap.ic_launcher_green,
                R.mipmap.ic_launcher_blue,
                R.mipmap.ic_launcher_violet,
                R.mipmap.ic_launcher_orange,
                R.mipmap.ic_launcher_pink,
                R.mipmap.ic_launcher_teal
        };
        int chosen = prefs.icon();
        for (int i = 0; i < icons.length; i++) {
            final int index = i;
            boolean active = i == chosen;
            ImageView image = new ImageView(this);
            image.setImageResource(icons[i]);
            image.setPadding(dp(3), dp(3), dp(3), dp(3));
            image.setBackground(glass(18, active
                    ? BadgeView.withAlpha(accent, 26)
                    : (style.night ? 0x0DFFFFFF : 0x08000000),
                    active ? accent : 0x00000000, false));
            image.setOnClickListener(v -> {
                prefs.setIcon(index);
                LauncherIcons.apply(this, index);
                recreate();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(58), dp(58));
            lp.rightMargin = dp(10);
            row.addView(image, lp);
        }
        card.addView(scroll);
        return card;
    }

    private LinearLayout backgroundSection() {
        LinearLayout card = section(getString(R.string.appearance_background),
                getString(R.string.appearance_background_hint));
        card.addView(chips(new String[]{
                getString(R.string.background_aurora),
                getString(R.string.background_gradient),
                getString(R.string.background_solid)
        }, prefs.background(), index -> {
            prefs.setBackground(index);
            recreate();
        }));
        return card;
    }

    private LinearLayout cardsSection() {
        LinearLayout card = section(getString(R.string.appearance_cards),
                getString(R.string.appearance_cards_hint));
        card.addView(chips(new String[]{
                getString(R.string.cards_glass),
                getString(R.string.cards_solid),
                getString(R.string.cards_outline)
        }, prefs.cards(), index -> {
            prefs.setCards(index);
            recreate();
        }));
        return card;
    }

    private LinearLayout shapeSection() {
        LinearLayout card = section(getString(R.string.appearance_shape),
                getString(R.string.appearance_shape_hint));

        TextView radiusValue = label(getString(R.string.radius_value, prefs.radius()),
                12, subColor, false);
        LinearLayout.LayoutParams valueLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        valueLp.topMargin = dp(10);
        valueLp.gravity = Gravity.END;
        card.addView(radiusValue, valueLp);

        SeekBar radius = new SeekBar(this);
        radius.setMax(28);
        radius.setProgress(prefs.radius());
        radius.setProgressTintList(android.content.res.ColorStateList.valueOf(accent));
        radius.setThumbTintList(android.content.res.ColorStateList.valueOf(accent));
        card.addView(radius, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        radius.setOnSeekBarChangeListener(new SimpleSeek() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int value, boolean fromUser) {
                radiusValue.setText(getString(R.string.radius_value, value));
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                prefs.setRadius(seekBar.getProgress());
                recreate();
            }
        });

        TextView fontValue = label(getString(R.string.font_value, prefs.fontScale()),
                12, subColor, false);
        LinearLayout.LayoutParams fontLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        fontLp.topMargin = dp(14);
        fontLp.gravity = Gravity.END;
        card.addView(fontValue, fontLp);

        SeekBar font = new SeekBar(this);
        font.setMax(40);
        font.setProgress(Math.max(0, Math.min(40, prefs.fontScale() - 85)));
        font.setProgressTintList(android.content.res.ColorStateList.valueOf(accent));
        font.setThumbTintList(android.content.res.ColorStateList.valueOf(accent));
        card.addView(font, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        font.setOnSeekBarChangeListener(new SimpleSeek() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int value, boolean fromUser) {
                fontValue.setText(getString(R.string.font_value, 85 + value));
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                prefs.setFontScale(85 + seekBar.getProgress());
                recreate();
            }
        });

        return card;
    }

    private LinearLayout densitySection() {
        LinearLayout card = section(getString(R.string.appearance_density),
                getString(R.string.appearance_density_hint));
        card.addView(chips(new String[]{
                getString(R.string.density_comfortable),
                getString(R.string.density_compact)
        }, prefs.density(), index -> {
            prefs.setDensity(index);
            recreate();
        }));
        return card;
    }

    // ----------------------------------------------------------------- styles

    private LinearLayout stylesSection() {
        LinearLayout card = section(getString(R.string.styles_title),
                getString(R.string.styles_hint));

        StyleManager manager = StyleManager.get(this);
        List<InstalledStyle> installed = manager.installed();

        TextView builtIn = styleRow(getString(R.string.styles_builtin),
                getString(R.string.styles_builtin_desc), "", null,
                prefs.styleId().isEmpty());
        builtIn.setOnClickListener(v -> {
            manager.select("");
            recreate();
        });
        LinearLayout.LayoutParams builtInLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        builtInLp.topMargin = dp(8);
        card.addView(builtIn, builtInLp);

        for (InstalledStyle item : installed) {
            boolean active = item.id.equals(prefs.styleId());
            String meta = (item.version.isEmpty() ? "" : "v" + item.version)
                    + (item.author.isEmpty() ? "" : (item.version.isEmpty() ? "" : "  ·  ")
                    + item.author);
            TextView row = styleRow(item.name, item.description, meta, item.icon, active);
            row.setOnClickListener(v -> {
                manager.select(item.id);
                recreate();
            });
            row.setOnLongClickListener(v -> {
                confirmRemove(manager, item);
                return true;
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(8);
            card.addView(row, lp);
        }

        if (!manager.lastError().isEmpty()) {
            TextView error = label(manager.lastError(), 11.5f, 0xFFFFB4AB, false);
            error.setLineSpacing(dp(2), 1f);
            LinearLayout.LayoutParams errorLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            errorLp.topMargin = dp(10);
            card.addView(error, errorLp);
        }

        TextView importButton = pill(getString(R.string.styles_import));
        importButton.setOnClickListener(v -> confirmImport());
        LinearLayout.LayoutParams importLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
        importLp.topMargin = dp(12);
        card.addView(importButton, importLp);

        TextView removeHint = label(getString(R.string.styles_remove_hint), 11, subColor, false);
        LinearLayout.LayoutParams hintLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hintLp.topMargin = dp(8);
        card.addView(removeHint, hintLp);

        return card;
    }

    private TextView styleRow(String name, String description, String meta, File icon,
                              boolean active) {
        TextView row = new TextView(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        row.setLineSpacing(dp(2), 1f);

        StringBuilder text = new StringBuilder(name);
        if (!TextUtils.isEmpty(meta)) {
            text.append("\n").append(meta);
        }
        if (!TextUtils.isEmpty(description)) {
            text.append("\n").append(description);
        }
        row.setText(text);
        row.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp(13));
        row.setTextColor(active ? onAccent : textColor);
        row.setBackground(active
                ? glass(18, accent, 0, false)
                : glass(18, style.night ? 0x10FFFFFF : 0x0A000000,
                style.night ? 0x22FFFFFF : 0x18000000, false));

        if (icon != null && icon.isFile()) {
            // A small icon is placed to the left through a compound drawable.
            try {
                android.graphics.Bitmap bitmap = BitmapFactory.decodeFile(icon.getAbsolutePath());
                if (bitmap != null) {
                    int size = dp(26);
                    android.graphics.drawable.BitmapDrawable drawable =
                            new android.graphics.drawable.BitmapDrawable(getResources(), bitmap);
                    drawable.setBounds(0, 0, size, size);
                    row.setCompoundDrawables(drawable, null, null, null);
                    row.setCompoundDrawablePadding(dp(10));
                }
            } catch (Throwable ignored) {
            }
        }
        return row;
    }

    private TextView pill(String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setGravity(Gravity.CENTER);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp(13));
        v.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        v.setTextColor(accent);
        v.setBackground(glass(22, BadgeView.withAlpha(accent, 14),
                BadgeView.withAlpha(accent, 42), false));
        return v;
    }

    private LinearLayout resetSection() {
        LinearLayout card = section(getString(R.string.appearance_reset),
                getString(R.string.appearance_reset_hint));
        TextView reset = label(getString(R.string.appearance_reset), 13, 0xFFFFB4AB, true);
        reset.setGravity(Gravity.CENTER);
        reset.setPadding(dp(14), dp(10), dp(14), dp(10));
        reset.setBackground(glass(22, 0x1AFFB4AB, 0x33FFB4AB, false));
        reset.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setMessage(R.string.appearance_reset_confirm)
                .setPositiveButton(R.string.appearance_reset, (d, w) -> {
                    prefs.resetAppearance();
                    recreate();
                })
                .setNegativeButton(R.string.cancel, null)
                .show());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(4);
        card.addView(reset, lp);
        return card;
    }

    // ------------------------------------------------------------- .ws import

    private void confirmImport() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.styles_warning_title)
                .setMessage(R.string.styles_warning)
                .setPositiveButton(R.string.styles_pick, (d, w) -> pickStyle())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void pickStyle() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivityForResult(intent, REQ_STYLE);
        } catch (Throwable e) {
            toast(getString(R.string.no_file));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_STYLE || resultCode != RESULT_OK || data == null
                || data.getData() == null) {
            return;
        }
        final Uri uri = data.getData();
        try {
            getContentResolver().takePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Throwable ignored) {
        }
        importStyle(uri);
    }

    private void importStyle(Uri uri) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(22), dp(18), dp(22), dp(6));

        TextView status = label(getString(R.string.styles_installing), 13.5f, textColor, true);
        box.addView(status);

        ProgressBar bar = new ProgressBar(this);
        bar.setIndeterminate(true);
        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(dp(28), dp(28));
        barLp.topMargin = dp(14);
        box.addView(bar, barLp);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(box)
                .setCancelable(false)
                .create();
        dialog.show();

        new Thread(() -> {
            String error = null;
            try {
                StyleManager.get(AppearanceActivity.this).install(uri);
            } catch (Throwable t) {
                error = t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage();
            }
            final String result = error;
            runOnUiThread(() -> {
                if (isFinishing()) {
                    return;
                }
                dialog.dismiss();
                if (result == null) {
                    recreate();
                } else {
                    new AlertDialog.Builder(AppearanceActivity.this)
                            .setTitle(R.string.styles_install_failed)
                            .setMessage(result)
                            .setPositiveButton(R.string.close, null)
                            .show();
                }
            });
        }, "ws-import").start();
    }

    private void confirmRemove(StyleManager manager, InstalledStyle item) {
        new AlertDialog.Builder(this)
                .setTitle(item.name)
                .setMessage(R.string.styles_remove_confirm)
                .setPositiveButton(R.string.styles_remove, (d, w) -> {
                    manager.remove(item.id);
                    recreate();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    private abstract static class SimpleSeek implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    }
}
