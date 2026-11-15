package com.wexresprise.store;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

/**
 * Small rounded label that shows a file extension (APK, PNG, MP4 ...) in its category colour.
 */
public class BadgeView extends View {

    private final Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private String label = "";
    private int tint = 0xFFA8C7FA;
    private float radius;
    private final float density;
    private final float baseTextSize;

    public BadgeView(Context c) {
        super(c);
        density = c.getResources().getDisplayMetrics().density;
        radius = 14 * density;
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(Math.max(1f, density));
        textPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        textPaint.setTextAlign(Paint.Align.CENTER);
        baseTextSize = 11 * density;
        textPaint.setTextSize(baseTextSize);
    }

    public void setLabel(String value) {
        label = value == null ? "" : value.toUpperCase();
        invalidate();
    }

    public void setTint(int color) {
        tint = color;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = (int) (46 * density);
        int h = (int) (46 * density);
        setMeasuredDimension(resolveSize(w, widthMeasureSpec), resolveSize(h, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        float inset = density;
        rect.set(inset, inset, w - inset, h - inset);

        bg.setColor(withAlpha(tint, 38));
        canvas.drawRoundRect(rect, radius, radius, bg);

        border.setColor(withAlpha(tint, 90));
        canvas.drawRoundRect(rect, radius, radius, border);

        textPaint.setColor(tint);
        textPaint.setTextSize(baseTextSize);
        String text = label;
        if (text.length() > 5) {
            text = text.substring(0, 5);
        }
        float maxWidth = w - 8 * density;
        if (textPaint.measureText(text) > maxWidth) {
            textPaint.setTextSize(8.5f * density);
        }
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float baseline = h / 2f - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(text, w / 2f, baseline, textPaint);
    }

    public static int withAlpha(int color, int percent) {
        int a = Math.max(0, Math.min(100, percent)) * 255 / 100;
        return (color & 0x00FFFFFF) | (a << 24);
    }
}
