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

    public void setAnalysis(List<Candle> source, MarketStructureAnalysis structure,
                            TradePlan plan, CandlestickPatternAnalysis patterns) {
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
        float right = getWidth() - dp(66);
        float top = dp(48);
        float bottom = getHeight() - dp(34);
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

        drawLegend(canvas, left, dp(10));
        drawGrid(canvas, left, right, top, bottom, min, max);
        drawCandles(canvas, left, right, top, bottom, min, max);
        drawPivots(canvas, left, right, top, bottom, min, max);

        if (structure != null) {
            drawPriceLine(canvas, left, right, top, bottom, min, max,
                    structure.getSupport(), "SUPORTE", Color.rgb(41, 121, 255), false, dp(1.8f));
            drawPriceLine(canvas, left, right, top, bottom, min, max,
                    structure.getResistance(), "RESIST.", Color.rgb(255, 143, 0), false, dp(1.8f));
            drawStructureEvent(canvas, right, top);
        }
        if (plan != null && plan.isExecutable()) {
            drawPriceLine(canvas, left, right, top, bottom, min, max,
                    plan.getEntryPrice(), "ENTRADA", Color.rgb(171, 71, 188), true, dp(1.5f));
            drawPriceLine(canvas, left, right, top, bottom, min, max,
                    plan.getStopLoss(), "STOP", Color.rgb(244, 67, 54), true, dp(1.5f));
            drawPriceLine(canvas, left, right, top, bottom, min, max,
                    plan.getTakeProfit(), "ALVO", Color.rgb(0, 200, 83), true, dp(1.5f));
        }
        drawPatternBadge(canvas, left, top);
        drawLatestPrice(canvas, left, right, top, bottom, min, max);
    }

    private void drawLegend(Canvas canvas, float left, float top) {
        textPaint.setTextSize(dp(8));
        textPaint.setColor(resolveColor(android.R.attr.textColorSecondary, Color.LTGRAY));
        String legend = "HH topo maior  •  HL fundo maior  •  LH topo menor  •  LL fundo menor";
        canvas.drawText(legend, left, top + dp(9), textPaint);
        canvas.drawText("BOS = rompimento  •  CHoCH = possível mudança de tendência", left, top + dp(22), textPaint);
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
            canvas.drawText(formatPrice(price), right + dp(5), y + dp(3), textPaint);
        }
    }

    private void drawCandles(Canvas canvas, float left, float right, float top, float bottom, double min, double max) {
        float slot = (right - left) / candles.size();
        float bodyWidth = Math.max(dp(3), Math.min(dp(9), slot * 0.74f));
        for (int i = 0; i < candles.size(); i++) {
            Candle candle = candles.get(i);
            float x = left + slot * i + slot / 2f;
            float highY = y(candle.getHigh(), top, bottom, min, max);
            float lowY = y(candle.getLow(), top, bottom, min, max);
            float openY = y(candle.getOpen(), top, bottom, min, max);
            float closeY = y(candle.getClose(), top, bottom, min, max);
            boolean bullish = candle.getClose() >= candle.getOpen();
            int color = bullish ? Color.rgb(0, 200, 83) : Color.rgb(244, 67, 54);

            paint.setColor(color);
            paint.setStrokeWidth(Math.max(dp(1.1f), slot * 0.12f));
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawLine(x, highY, x, lowY, paint);

            paint.setStyle(Paint.Style.FILL);
            float bodyTop = Math.min(openY, closeY);
            float bodyBottom = Math.max(openY, closeY);
            if (bodyBottom - bodyTop < dp(2)) bodyBottom = bodyTop + dp(2);
            canvas.drawRoundRect(new RectF(x - bodyWidth / 2f, bodyTop, x + bodyWidth / 2f, bodyBottom),
                    dp(0.8f), dp(0.8f), paint);
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
                               double min, double max, double price, String label, int color,
                               boolean dashed, float strokeWidth) {
        if (!Double.isFinite(price) || price <= 0 || price < min || price > max) return;
        float lineY = y(price, top, bottom, min, max);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(strokeWidth);
        paint.setColor(color);
        paint.setPathEffect(dashed ? new DashPathEffect(new float[]{dp(7), dp(5)}, 0) : null);
        canvas.drawLine(left, lineY, right, lineY, paint);
        paint.setPathEffect(null);
        textPaint.setColor(color);
        textPaint.setTextSize(dp(8));
        canvas.drawText(label, left + dp(3), lineY - dp(4), textPaint);
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
        canvas.drawText(event, right - dp(58), top + dp(14), textPaint);
    }

    private void drawPatternBadge(Canvas canvas, float left, float top) {
        if (patterns == null || patterns.getPatterns() == null || patterns.getPatterns().isEmpty()) return;
        String label = patterns.getPatterns().get(0);
        if (label.length() > 24) label = label.substring(0, 24) + "…";
        textPaint.setTextSize(dp(9));
        float width = textPaint.measureText(label) + dp(12);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(withAlpha(resolveColor(android.R.attr.colorAccent, Color.MAGENTA), 58));
        canvas.drawRoundRect(new RectF(left, top, left + width, top + dp(20)), dp(8), dp(8), paint);
        textPaint.setColor(resolveColor(android.R.attr.textColorPrimary, Color.WHITE));
        canvas.drawText(label, left + dp(6), top + dp(14), textPaint);
    }

    private void drawLatestPrice(Canvas canvas, float left, float right, float top, float bottom, double min, double max) {
        Candle latest = candles.get(candles.size() - 1);
        float priceY = y(latest.getClose(), top, bottom, min, max);
        int color = latest.getClose() >= latest.getOpen() ? Color.rgb(0, 200, 83) : Color.rgb(244, 67, 54);

        paint.setColor(withAlpha(color, 145));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1));
        paint.setPathEffect(new DashPathEffect(new float[]{dp(3), dp(4)}, 0));
        canvas.drawLine(left, priceY, right, priceY, paint);
        paint.setPathEffect(null);

        String label = formatPrice(latest.getClose());
        textPaint.setTextSize(dp(9));
        float width = textPaint.measureText(label) + dp(10);
        RectF tag = new RectF(right + dp(2), priceY - dp(10), right + dp(2) + width, priceY + dp(10));
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        canvas.drawRoundRect(tag, dp(4), dp(4), paint);
        textPaint.setColor(Color.WHITE);
        canvas.drawText(label, tag.left + dp(5), priceY + dp(3), textPaint);
    }

    private String formatPrice(double price) {
        double abs = Math.abs(price);
        int decimals = abs >= 1000 ? 2 : abs >= 10 ? 3 : 5;
        return String.format(Locale.US, "%." + decimals + "f", price);
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
