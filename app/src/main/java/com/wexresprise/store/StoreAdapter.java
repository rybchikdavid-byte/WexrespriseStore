package com.wexresprise.store;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Glass file cards for the ListView. */
public class StoreAdapter extends BaseAdapter {

    public interface Listener {
        void onDownload(StoreItem item);

        void onDetails(StoreItem item);
    }

    private final Context context;
    private final Listener listener;
    private final List<StoreItem> items = new ArrayList<>();
    private final int primary;
    private final int textColor;
    private final int subColor;
    private final float density;

    public StoreAdapter(Context c, Listener listener, int accent) {
        this.context = c;
        this.listener = listener;
        this.density = c.getResources().getDisplayMetrics().density;
        this.primary = accent;
        this.textColor = c.getColor(R.color.wx_text);
        this.subColor = c.getColor(R.color.wx_sub);
    }

    public void setItems(List<StoreItem> list) {
        items.clear();
        if (list != null) {
            items.addAll(list);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public StoreItem getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Holder h;
        if (convertView == null) {
            convertView = build();
            h = (Holder) convertView.getTag();
        } else {
            h = (Holder) convertView.getTag();
        }
        StoreItem item = getItem(position);

        h.badge.setLabel(labelFor(item));
        h.badge.setTint(tintFor(item));
        h.title.setText(item.title);

        if (TextUtils.isEmpty(item.description)) {
            h.description.setVisibility(View.GONE);
        } else {
            h.description.setVisibility(View.VISIBLE);
            h.description.setText(item.description);
        }

        String size = Formats.size(item.size);
        String date = Formats.date(item.createdAt);
        String meta = date.isEmpty() ? size : size + "  ·  " + date;
        if (item.belongsToProjects()) {
            meta = meta + "  ·  Wexresprise";
        }
        h.meta.setText(meta);

        convertView.setOnClickListener(v -> listener.onDetails(item));
        h.download.setOnClickListener(v -> listener.onDownload(item));
        return convertView;
    }

    private View build() {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(12));
        int fill = 0x14FFFFFF;
        int stroke = 0x2EFFFFFF;
        GlassDrawable bg = new GlassDrawable(dp(22), fill, stroke, Math.max(1f, density));
        bg.setGlossAlpha(0x30);
        card.setBackground(bg);
        card.setElevation(dp(2));
        card.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout top = new LinearLayout(context);
        top.setOrientation(LinearLayout.HORIZONTAL);
        card.addView(top, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        BadgeView badge = new BadgeView(context);
        LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(dp(46), dp(46));
        top.addView(badge, badgeLp);

        LinearLayout texts = new LinearLayout(context);
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textsLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textsLp.leftMargin = dp(12);
        top.addView(texts, textsLp);

        TextView title = new TextView(context);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        title.setTextColor(textColor);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        title.setMaxLines(2);
        title.setEllipsize(TextUtils.TruncateAt.END);
        texts.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView description = new TextView(context);
        description.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        description.setTextColor(subColor);
        description.setMaxLines(2);
        description.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams descLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        descLp.topMargin = dp(3);
        texts.addView(description, descLp);

        LinearLayout bottom = new LinearLayout(context);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams bottomLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bottomLp.topMargin = dp(11);
        card.addView(bottom, bottomLp);

        TextView meta = new TextView(context);
        meta.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        meta.setTextColor(subColor);
        meta.setSingleLine(true);
        meta.setEllipsize(TextUtils.TruncateAt.END);
        bottom.addView(meta, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout download = new LinearLayout(context);
        download.setOrientation(LinearLayout.HORIZONTAL);
        download.setGravity(Gravity.CENTER);
        download.setPadding(dp(12), dp(8), dp(14), dp(8));
        GlassDrawable dl = new GlassDrawable(dp(18), BadgeView.withAlpha(primary, 16),
                BadgeView.withAlpha(primary, 40), Math.max(1f, density));
        dl.setGloss(false);
        download.setBackground(dl);

        ImageView icon = new ImageView(context);
        icon.setImageResource(R.drawable.ic_download);
        icon.setColorFilter(primary);
        download.addView(icon, new LinearLayout.LayoutParams(dp(15), dp(15)));

        TextView label = new TextView(context);
        label.setText(R.string.download);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        label.setTextColor(primary);
        label.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        labelLp.leftMargin = dp(6);
        download.addView(label, labelLp);

        bottom.addView(download, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        Holder h = new Holder();
        h.badge = badge;
        h.title = title;
        h.description = description;
        h.meta = meta;
        h.download = download;
        card.setTag(h);
        return card;
    }

    private String labelFor(StoreItem item) {
        String ext = item.extension();
        if (ext.isEmpty()) {
            ext = Formats.extensionForMime(item.mime);
        }
        return ext.isEmpty() ? "FILE" : ext;
    }

    private int tintFor(StoreItem item) {
        String ext = labelFor(item).toLowerCase(Locale.ROOT);
        String mime = item.mime.toLowerCase(Locale.ROOT);
        if (ext.equals("apk") || ext.equals("xapk") || ext.equals("apks")) {
            return 0xFF7BD88F;
        }
        if (mime.startsWith("image/") || ext.equals("png") || ext.equals("jpg")
                || ext.equals("jpeg") || ext.equals("webp") || ext.equals("gif")) {
            return 0xFFA8C7FA;
        }
        if (mime.startsWith("video/") || ext.equals("mp4") || ext.equals("mkv")
                || ext.equals("mov") || ext.equals("webm")) {
            return 0xFFFFB4AB;
        }
        if (mime.startsWith("audio/") || ext.equals("mp3") || ext.equals("m4a")
                || ext.equals("ogg") || ext.equals("wav") || ext.equals("flac")) {
            return 0xFFE8B9FF;
        }
        if (ext.equals("zip") || ext.equals("rar") || ext.equals("7z") || ext.equals("tar")
                || ext.equals("gz")) {
            return 0xFF9BE7E0;
        }
        if (mime.startsWith("text/") || ext.equals("pdf") || ext.equals("doc")
                || ext.equals("docx") || ext.equals("xls") || ext.equals("xlsx")
                || ext.equals("ppt") || ext.equals("pptx") || ext.equals("md")) {
            return 0xFFFFD28C;
        }
        return 0xFFB8C4D6;
    }

    private int dp(float value) {
        return Math.round(value * density);
    }

    private static class Holder {
        BadgeView badge;
        TextView title;
        TextView description;
        TextView meta;
        LinearLayout download;
    }
}
