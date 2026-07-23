package com.orbistrade.app.domain.structure;

import com.orbistrade.app.data.market.Candle;

import java.util.ArrayList;
import java.util.List;

public final class MarketStructureAnalyzer {
    private static final int PIVOT_WINDOW = 2;

    public MarketStructureAnalysis analyze(List<Candle> candles) {
        if (candles == null || candles.size() < 30) {
            throw new IllegalArgumentException("São necessários ao menos 30 candles para analisar estrutura.");
        }

        List<Pivot> highs = new ArrayList<>();
        List<Pivot> lows = new ArrayList<>();
        for (int i = PIVOT_WINDOW; i < candles.size() - PIVOT_WINDOW; i++) {
            Candle current = candles.get(i);
            if (isSwingHigh(candles, i)) highs.add(new Pivot(i, current.getHigh()));
            if (isSwingLow(candles, i)) lows.add(new Pivot(i, current.getLow()));
        }

        if (highs.size() < 2 || lows.size() < 2) {
            double high = highest(candles);
            double low = lowest(candles);
            List<String> evidence = new ArrayList<>();
            evidence.add("Poucos pivôs confirmados; estrutura ainda indefinida.");
            return new MarketStructureAnalysis(
                    MarketStructureAnalysis.Trend.UNDEFINED,
                    MarketStructureAnalysis.Event.NONE,
                    "--",
                    "--",
                    high,
                    low,
                    false,
                    false,
                    25,
                    evidence
            );
        }

        Pivot previousHigh = highs.get(highs.size() - 2);
        Pivot lastHigh = highs.get(highs.size() - 1);
        Pivot previousLow = lows.get(lows.size() - 2);
        Pivot lastLow = lows.get(lows.size() - 1);

        double tolerance = averageRange(candles, 14) * 0.35;
        String highLabel = lastHigh.price > previousHigh.price + tolerance ? "HH"
                : lastHigh.price < previousHigh.price - tolerance ? "LH" : "EH";
        String lowLabel = lastLow.price > previousLow.price + tolerance ? "HL"
                : lastLow.price < previousLow.price - tolerance ? "LL" : "EL";

        MarketStructureAnalysis.Trend trend;
        if ("HH".equals(highLabel) && "HL".equals(lowLabel)) {
            trend = MarketStructureAnalysis.Trend.BULLISH;
        } else if ("LH".equals(highLabel) && "LL".equals(lowLabel)) {
            trend = MarketStructureAnalysis.Trend.BEARISH;
        } else {
            trend = MarketStructureAnalysis.Trend.RANGE;
        }

        Candle latest = candles.get(candles.size() - 1);
        Candle previous = candles.get(candles.size() - 2);
        MarketStructureAnalysis.Event event = detectEvent(
                trend,
                latest,
                previous,
                previousHigh.price,
                previousLow.price,
                tolerance
        );

        boolean equalHighs = Math.abs(lastHigh.price - previousHigh.price) <= tolerance;
        boolean equalLows = Math.abs(lastLow.price - previousLow.price) <= tolerance;

        List<String> evidence = new ArrayList<>();
        evidence.add("Último topo: " + highLabel + "; último fundo: " + lowLabel + ".");
        if (event != MarketStructureAnalysis.Event.NONE) {
            evidence.add("Evento estrutural detectado: " + event.name().replace('_', ' ') + ".");
        }
        if (equalHighs) evidence.add("Topos equivalentes sugerem liquidez acima da resistência.");
        if (equalLows) evidence.add("Fundos equivalentes sugerem liquidez abaixo do suporte.");

        String pullback = detectPullback(trend, latest, lastHigh.price, lastLow.price, tolerance);
        if (!pullback.isEmpty()) evidence.add(pullback);

        String falseBreakout = detectFalseBreakout(latest, previousHigh.price, previousLow.price, tolerance);
        if (!falseBreakout.isEmpty()) evidence.add(falseBreakout);

        int confidence = 45;
        if (trend != MarketStructureAnalysis.Trend.RANGE) confidence += 20;
        if (event != MarketStructureAnalysis.Event.NONE) confidence += 15;
        if (highs.size() >= 4 && lows.size() >= 4) confidence += 10;
        if (equalHighs || equalLows) confidence += 5;

        return new MarketStructureAnalysis(
                trend,
                event,
                highLabel,
                lowLabel,
                lastHigh.price,
                lastLow.price,
                equalHighs,
                equalLows,
                confidence,
                evidence
        );
    }

    private boolean isSwingHigh(List<Candle> candles, int index) {
        double value = candles.get(index).getHigh();
        for (int offset = 1; offset <= PIVOT_WINDOW; offset++) {
            if (value <= candles.get(index - offset).getHigh()
                    || value <= candles.get(index + offset).getHigh()) return false;
        }
        return true;
    }

    private boolean isSwingLow(List<Candle> candles, int index) {
        double value = candles.get(index).getLow();
        for (int offset = 1; offset <= PIVOT_WINDOW; offset++) {
            if (value >= candles.get(index - offset).getLow()
                    || value >= candles.get(index + offset).getLow()) return false;
        }
        return true;
    }

    private MarketStructureAnalysis.Event detectEvent(
            MarketStructureAnalysis.Trend trend,
            Candle latest,
            Candle previous,
            double priorHigh,
            double priorLow,
            double tolerance
    ) {
        boolean crossedHigh = previous.getClose() <= priorHigh && latest.getClose() > priorHigh + tolerance;
        boolean crossedLow = previous.getClose() >= priorLow && latest.getClose() < priorLow - tolerance;

        if (crossedHigh) {
            return trend == MarketStructureAnalysis.Trend.BEARISH
                    ? MarketStructureAnalysis.Event.CHOCH_BULLISH
                    : MarketStructureAnalysis.Event.BOS_BULLISH;
        }
        if (crossedLow) {
            return trend == MarketStructureAnalysis.Trend.BULLISH
                    ? MarketStructureAnalysis.Event.CHOCH_BEARISH
                    : MarketStructureAnalysis.Event.BOS_BEARISH;
        }
        return MarketStructureAnalysis.Event.NONE;
    }

    private String detectPullback(
            MarketStructureAnalysis.Trend trend,
            Candle latest,
            double resistance,
            double support,
            double tolerance
    ) {
        if (trend == MarketStructureAnalysis.Trend.BULLISH
                && latest.getLow() <= support + tolerance
                && latest.getClose() > support) {
            return "Pullback comprador reagindo próximo ao suporte estrutural.";
        }
        if (trend == MarketStructureAnalysis.Trend.BEARISH
                && latest.getHigh() >= resistance - tolerance
                && latest.getClose() < resistance) {
            return "Pullback vendedor rejeitando a resistência estrutural.";
        }
        return "";
    }

    private String detectFalseBreakout(Candle latest, double resistance, double support, double tolerance) {
        if (latest.getHigh() > resistance + tolerance && latest.getClose() < resistance) {
            return "Possível falso rompimento acima da resistência.";
        }
        if (latest.getLow() < support - tolerance && latest.getClose() > support) {
            return "Possível falso rompimento abaixo do suporte.";
        }
        return "";
    }

    private double averageRange(List<Candle> candles, int period) {
        int start = Math.max(0, candles.size() - period);
        double sum = 0.0;
        for (int i = start; i < candles.size(); i++) {
            sum += candles.get(i).getHigh() - candles.get(i).getLow();
        }
        return sum / Math.max(1, candles.size() - start);
    }

    private double highest(List<Candle> candles) {
        double value = -Double.MAX_VALUE;
        for (Candle candle : candles) value = Math.max(value, candle.getHigh());
        return value;
    }

    private double lowest(List<Candle> candles) {
        double value = Double.MAX_VALUE;
        for (Candle candle : candles) value = Math.min(value, candle.getLow());
        return value;
    }

    private static final class Pivot {
        private final int index;
        private final double price;

        private Pivot(int index, double price) {
            this.index = index;
            this.price = price;
        }
    }
}
