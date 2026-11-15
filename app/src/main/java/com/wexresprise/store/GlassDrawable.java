package com.wexresprise.store;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/**
 * Rounded "liquid glass" panel: translucent fill, a soft top gloss and a thin light stroke.
 */
public class GlassDrawable extends Drawable {

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glossPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final float radius;
    private final int fillColor;
    private final int strokeColor;
    private final float strokeWidth;
    private boolean gloss;
    private int glossAlpha = 0x40;

    public GlassDrawable(float radius, int fillColor, int strokeColor, float strokeWidth) {
        this.radius = radius;
        this.fillColor = fillColor;
        this.strokeColor = strokeColor;
        this.strokeWidth = strokeWidth;
        fill.setStyle(Paint.Style.FILL);
        glossPaint.setStyle(Paint.Style.FILL);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(strokeWidth);
    }

    public GlassDrawable setGloss(boolean gloss) {
        this.gloss = gloss;
        invalidateSelf();
        return this;
    }

    public GlassDrawable setGlossAlpha(int alpha) {
        this.glossAlpha = Math.max(0, Math.min(255, alpha));
        invalidateSelf();
        return this;
    }

    @Override
    public void draw(Canvas canvas) {
        Rect b = getBounds();
        rect.set(b);
        fill.setColor(fillColor);
        canvas.drawRoundRect(rect, radius, radius, fill);

        if (gloss) {
            glossPaint.setShader(new LinearGradient(
                    0, b.top, 0, b.top + b.height() * 0.7f,
                    (glossAlpha << 24) | 0x00FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
            canvas.drawRoundRect(rect, radius, radius, glossPaint);
        }

        if (strokeWidth > 0) {
            stroke.setColor(strokeColor);
            rect.inset(strokeWidth / 2f, strokeWidth / 2f);
            canvas.drawRoundRect(rect, radius, radius, stroke);
            rect.inset(-strokeWidth / 2f, -strokeWidth / 2f);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        fill.setAlpha(alpha);
        glossPaint.setAlpha(alpha);
        stroke.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        fill.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
