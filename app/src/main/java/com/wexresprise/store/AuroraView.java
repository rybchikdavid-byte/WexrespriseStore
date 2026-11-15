package com.wexresprise.store;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

/**
 * Dark background with two slow-moving aurora glows. Everything is allocated once, only the
 * canvas translation changes, so it stays cheap enough for a phone.
 */
public class AuroraView extends View {

    private static final int TICK = 40;

    private final Paint base = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowA = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowB = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint veil = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private float phase;
    private boolean running;
    private int accent = 0xFFA8C7FA;

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (!running) {
                return;
            }
            phase += 0.004f;
            if (phase > 1f) {
                phase -= 1f;
            }
            invalidate();
            handler.postDelayed(this, TICK);
        }
    };

    public AuroraView(Context c) {
        super(c);
    }

    public void setAccent(int color) {
        accent = color;
        rebuild();
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        rebuild();
    }

    private void rebuild() {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        base.setShader(new LinearGradient(0, 0, w * 0.4f, h,
                0xFF0B0D12, 0xFF101420, Shader.TileMode.CLAMP));

        int blue = 0xFF4E7BFF;
        int violet = 0xFF9A6BFF;
        glowA.setShader(new RadialGradient(0, 0, h * 0.55f,
                new int[]{withAlpha(blend(blue, accent, 0.35f), 60), 0x00000000},
                new float[]{0f, 1f}, Shader.TileMode.CLAMP));
        glowB.setShader(new RadialGradient(0, 0, h * 0.5f,
                new int[]{withAlpha(violet, 48), 0x00000000},
                new float[]{0f, 1f}, Shader.TileMode.CLAMP));

        veil.setShader(new LinearGradient(0, 0, 0, h,
                0x660B0D12, 0xCC0B0D12, Shader.TileMode.CLAMP));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        canvas.drawRect(0, 0, w, h, base);

        float x1 = w * (0.12f + 0.10f * (float) Math.sin(phase * Math.PI * 2));
        float y1 = h * (0.06f + 0.05f * (float) Math.cos(phase * Math.PI * 2));
        canvas.save();
        canvas.translate(x1, y1);
        canvas.drawCircle(0, 0, h * 0.55f, glowA);
        canvas.restore();

        float x2 = w * (0.92f - 0.08f * (float) Math.cos(phase * Math.PI * 2 + 1.2f));
        float y2 = h * (0.68f + 0.06f * (float) Math.sin(phase * Math.PI * 2 + 0.6f));
        canvas.save();
        canvas.translate(x2, y2);
        canvas.drawCircle(0, 0, h * 0.5f, glowB);
        canvas.restore();

        canvas.drawRect(0, 0, w, h, veil);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        start();
    }

    @Override
    protected void onDetachedFromWindow() {
        stop();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == VISIBLE) {
            start();
        } else {
            stop();
        }
    }

    private void start() {
        if (running || getVisibility() != VISIBLE) {
            return;
        }
        running = true;
        handler.postDelayed(ticker, TICK);
    }

    private void stop() {
        running = false;
        handler.removeCallbacks(ticker);
    }

    private static int withAlpha(int color, int percent) {
        return (color & 0x00FFFFFF) | (((percent * 255 / 100) & 0xFF) << 24);
    }

    private static int blend(int a, int b, float t) {
        int r = (int) ((a >> 16 & 0xFF) + ((b >> 16 & 0xFF) - (a >> 16 & 0xFF)) * t);
        int g = (int) ((a >> 8 & 0xFF) + ((b >> 8 & 0xFF) - (a >> 8 & 0xFF)) * t);
        int bl = (int) ((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }
}
