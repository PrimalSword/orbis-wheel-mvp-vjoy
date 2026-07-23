package com.orbistrade.app.domain.indicator;

import com.orbistrade.app.data.market.Candle;

import java.util.List;

public final class TechnicalIndicatorCalculator {

    public double ema(List<Candle> candles, int period) {
        validate(candles, period);
        double multiplier = 2.0 / (period + 1.0);
        double ema = candles.get(0).getClose();
        for (int index = 1; index < candles.size(); index++) {
            ema = ((candles.get(index).getClose() - ema) * multiplier) + ema;
        }
        return ema;
    }

    public double rsi(List<Candle> candles, int period) {
        validate(candles, period + 1);
        int start = candles.size() - period;
        double gains = 0.0;
        double losses = 0.0;

        for (int index = start; index < candles.size(); index++) {
            double change = candles.get(index).getClose() - candles.get(index - 1).getClose();
            if (change >= 0) {
                gains += change;
            } else {
                losses += Math.abs(change);
            }
        }

        if (losses == 0.0) {
            return 100.0;
        }
        double relativeStrength = (gains / period) / (losses / period);
        return 100.0 - (100.0 / (1.0 + relativeStrength));
    }

    public double atrPercent(List<Candle> candles, int period) {
        validate(candles, period + 1);
        int start = candles.size() - period;
        double trueRangeSum = 0.0;

        for (int index = start; index < candles.size(); index++) {
            Candle current = candles.get(index);
            double previousClose = candles.get(index - 1).getClose();
            double range = current.getHigh() - current.getLow();
            double highGap = Math.abs(current.getHigh() - previousClose);
            double lowGap = Math.abs(current.getLow() - previousClose);
            trueRangeSum += Math.max(range, Math.max(highGap, lowGap));
        }

        double atr = trueRangeSum / period;
        double latestClose = candles.get(candles.size() - 1).getClose();
        return latestClose == 0.0 ? 0.0 : (atr / latestClose) * 100.0;
    }

    private void validate(List<Candle> candles, int minimumSize) {
        if (candles == null || candles.size() < minimumSize) {
            throw new IllegalArgumentException("Histórico insuficiente para calcular indicadores.");
        }
    }
}