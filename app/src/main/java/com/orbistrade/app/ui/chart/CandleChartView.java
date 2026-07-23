package com.orbistrade.app.ui.chart;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.orbistrade.app.data.market.Candle;
import com.orbistrade.app.domain.pattern.CandlestickPatternAnalysis;
import com.orbistrade.app.domain.risk.TradePlan;
import com.orbistrade.app.domain.structure.MarketStructureAnalysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class CandleChartView extends View {
    private static final int MAX_CANDLES = 96;
    private static final int PIVOT_WINDOW = 3;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<Candle> candles = new ArrayList<>();

    private MarketStructureAnalysis structure;
    private TradePlan plan;
    private CandlestickPatternAnalysis patterns;

    public CandleChartView(Context context) {
        this(context, null);
    }

    public CandleChartView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CandleChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        textPaint.setTextSize(dp(10));
        textPaint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));
        setContentDescription("Gráfico de candles de cinco minutos com estrutura, suporte, resistência, entrada, stop e alvo.");
    }

    public void setAnalysis(
            List<Candle> source,
            MarketStructureAnalysis structure,
            TradePlan plan,
            CandlestickPatternAnalysis patterns
    ) {
        candles.clear();
        if (source != null && !source.isEmpty()) {
            int start = Math.max(0, source.size() - MAX_CANDLES);
            candles.addAll(source.subList(start, source.size()));
        }
        this.structure = structure;
        this.plan = plan;
        this.patterns = patterns;
        invalidate();
    }

    public void clearAnalysis() {
        candles.clear();
        structure = null;
        plan = null;
        patterns = null;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(resolveColor(android.R.attr.colorBackground, Color.rgb(18, 18, 18)));

        if (candles.size() < 2) {
            drawCenteredMessage(canvas, "Gráfico aguardando análise");
            return;
        }

        float left = dp(12);
        float right = getWidth() - dp(58);
        float top = dp(22);
        float bottom = getHeight() - dp(28);
        if (right <= left || bottom <= top) return;

        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (Candle candle : candles) {
            min = Math.min(min, candle.getLow());
            max = Math.max(max, candle.getHigh());
        }
        if (structure != null) {
            if (structure.getSupport() > 0) min = Math.min(min, structure.getSupport());
            if (structure.getResistance() > 0) max = Math.max(max, structure.getResistance());
        }
        if (plan != null && plan.isExecutable()) {
            min = Math.min(min, Math.min(plan.getStopLoss(), plan.getTakeProfit()));
            max = Math.max(max, Math.max(plan.getStopLoss(), plan.getTakeProfit()));
        }

        double range = Math.max(max - min, Math.max(Math.abs(max) * 0.0005, 0.00001));
        min -= range * 0.08;
        max += range * 0.08;

        drawGrid(canvas, left, right, top, bottom, min, max);
        drawCandles(canvas, left, right, top, bottom, min, max);
        drawPivots(canvas, left, right, top, bottom, min, max);

        if (structure != null) {
            drawPriceLine(canvas, left, right, top, bottom, min, max, structure.getSupport(), "SUP", Color.rgb(66, 165, 245), false);
            drawPriceLine(canvas, left, right, top, bottom, min, max, structure.getResistance(), "RES", Color.rgb(255, 167, 38), false);
            drawStructureEvent(canvas, right, top);
        }
        if (plan != null && plan.isExecutable()) {
            drawPriceLine(canvas, left, right, top, bottom, min, max, plan.getEntryPrice(), "ENTRADA", Color.rgb(171, 71, 188), true);
            drawPriceLine(canvas, left, right, top, bottom, min, max, plan.getStopLoss(), "STOP", Color.rgb(239, 83, 80), true);
            drawPriceLine(canvas, left, right, top, bottom, min, max, plan.getTakeProfit(), "ALVO", Color.rgb(38, 166, 154), true);
        }
        drawPatternBadge(canvas, left, top);
        drawLatestPrice(canvas, right, top, bottom, min, max);
    }

    private void drawGrid(Canvas canvas, float left, float right, float top, float bottom, double min, double max) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(0.6f));
        paint.setColor(withAlpha(resolveColor(android.R.attr.textColorSecondary, Color.GRAY), 55));
        paint.setPathEffect(null);
        textPaint.setColor(resolveColor(android.R.attr.textColorSecondary, Color.LTGRAY));
        textPaint.setTextSize(dp(9));
        for (int i = 0; i <= 4; i++) {
            float y = top + (bottom - top) * i / 4f;
            canvas.drawLine(left, y, right, y, paint);
            double price = max - (max - min) * i / 4d;
            canvas.drawText(String.format(Locale.US, "%.5f", price), right + dp(4), y + dp(3), textPaint);
        }
    }

    private void drawCandles(Canvas canvas, float left, float right, float top, float bottom, double min, double max) {
        float slot = (right - left) / candles.size();
        float bodyWidth = Math.max(dp(2), slot * 0.62f);
        for (int i = 0; i < candles.size(); i++) {
            Candle candle = candles.get(i);
            float x = left + slot * i + slot / 2f;
            float highY = y(candle.getHigh(), top, bottom, min, max);
            float lowY = y(candle.getLow(), top, bottom, min, max);
            float openY = y(candle.getOpen(), top, bottom, min, max);
            float closeY = y(candle.getClose(), top, bottom, min, max);
            boolean bullish = candle.getClose() >= candle.getOpen();
            int color = bullish ? Color.rgb(38, 166, 154) : Color.rgb(239, 83, 80);

            paint.setColor(color);
            paint.setStrokeWidth(Math.max(dp(0.8f), slot * 0.08f));
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawLine(x, highY, x, lowY, paint);

            paint.setStyle(Paint.Style.FILL);
            float bodyTop = Math.min(openY, closeY);
            float bodyBottom = Math.max(openY, closeY);
            if (bodyBottom - bodyTop < dp(1.5f)) bodyBottom = bodyTop + dp(1.5f);
            canvas.drawRect(new RectF(x - bodyWidth / 2f, bodyTop, x + bodyWidth / 2f, bodyBottom), paint);
        }
    }

    private void drawPivots(Canvas canvas, float left, float right, float top, float bottom, double min, double max) {
        if (candles.size() < PIVOT_WINDOW * 2 + 1) return;
        float slot = (right - left) / candles.size();
        Double previousHigh = null;
        Double previousLow = null;
        textPaint.setTextSize(dp(8));

        for (int i = PIVOT_WINDOW; i < candles.size() - PIVOT_WINDOW; i++) {
            Candle candle = candles.get(i);
            boolean pivotHigh = true;
            boolean pivotLow = true;
            for (int j = i - PIVOT_WINDOW; j <= i + PIVOT_WINDOW; j++) {
                if (j == i) continue;
                pivotHigh &= candle.getHigh() > candles.get(j).getHigh();
                pivotLow &= candle.getLow() < candles.get(j).getLow();
            }
            float x = left + slot * i + slot / 2f;
            if (pivotHigh) {
                String label = previousHigh == null || candle.getHigh() > previousHigh ? "HH" : "LH";
                textPaint.setColor(Color.rgb(255, 167, 38));
                canvas.drawText(label, x - dp(6), y(candle.getHigh(), top, bottom, min, max) - dp(5), textPaint);
                previousHigh = candle.getHigh();
            }
            if (pivotLow) {
                String label = previousLow == null || candle.getLow() > previousLow ? "HL" : "LL";
                textPaint.setColor(Color.rgb(66, 165, 245));
                canvas.drawText(label, x - dp(6), y(candle.getLow(), top, bottom, min, max) + dp(12), textPaint);
                previousLow = candle.getLow();
            }
        }
    }

    private void drawPriceLine(Canvas canvas, float left, float right, float top, float bottom,
                               double min, double max, double price, String label, int color, boolean dashed) {
        if (!Double.isFinite(price) || price <= 0 || price < min || price > max) return;
        float y = y(price, top, bottom, min, max);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1));
        paint.setColor(color);
        paint.setPathEffect(dashed ? new DashPathEffect(new float[]{dp(7), dp(5)}, 0) : null);
        canvas.drawLine(left, y, right, y, paint);
        paint.setPathEffect(null);
        textPaint.setColor(color);
        textPaint.setTextSize(dp(8));
        canvas.drawText(label, left + dp(3), y - dp(3), textPaint);
    }

    private void drawStructureEvent(Canvas canvas, float right, float top) {
        String event;
        switch (structure.getEvent()) {
            case BOS_BULLISH: event = "BOS ↑"; break;
            case BOS_BEARISH: event = "BOS ↓"; break;
            case CHOCH_BULLISH: event = "CHoCH ↑"; break;
            case CHOCH_BEARISH: event = "CHoCH ↓"; break;
            default: return;
        }
        textPaint.setColor(resolveColor(android.R.attr.textColorPrimary, Color.WHITE));
        textPaint.setTextSize(dp(10));
        canvas.drawText(event, right - dp(54), top + dp(12), textPaint);
    }

    private void drawPatternBadge(Canvas canvas, float left, float top) {
        if (patterns == null || patterns.getPatterns() == null || patterns.getPatterns().isEmpty()) return;
        String label = patterns.getPatterns().get(0);
        if (label.length() > 24) label = label.substring(0, 24) + "…";
        textPaint.setTextSize(dp(9));
        float width = textPaint.measureText(label) + dp(12);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(withAlpha(resolveColor(android.R.attr.colorAccent, Color.MAGENTA), 48));
        canvas.drawRoundRect(new RectF(left, top, left + width, top + dp(20)), dp(8), dp(8), paint);
        textPaint.setColor(resolveColor(android.R.attr.textColorPrimary, Color.WHITE));
        canvas.drawText(label, left + dp(6), top + dp(14), textPaint);
    }

    private void drawLatestPrice(Canvas canvas, float right, float top, float bottom, double min, double max) {
        Candle latest = candles.get(candles.size() - 1);
        float y = y(latest.getClose(), top, bottom, min, max);
        paint.setColor(resolveColor(android.R.attr.colorAccent, Color.CYAN));
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(right, y, dp(3), paint);
    }

    private void drawCenteredMessage(Canvas canvas, String message) {
        textPaint.setColor(resolveColor(android.R.attr.textColorSecondary, Color.LTGRAY));
        textPaint.setTextSize(dp(13));
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(message, getWidth() / 2f, getHeight() / 2f, textPaint);
        textPaint.setTextAlign(Paint.Align.LEFT);
    }

    private float y(double price, float top, float bottom, double min, double max) {
        return (float) (bottom - ((price - min) / (max - min)) * (bottom - top));
    }

    private int resolveColor(int attribute, int fallback) {
        android.util.TypedValue value = new android.util.TypedValue();
        if (getContext().getTheme().resolveAttribute(attribute, value, true)) {
            if (value.resourceId != 0) return getContext().getColor(value.resourceId);
            return value.data;
        }
        return fallback;
    }

    private int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
