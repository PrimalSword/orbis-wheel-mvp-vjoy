package com.orbistrade.app.domain.structure;

import com.orbistrade.app.data.market.Candle;

import java.util.ArrayList;
import java.util.List;

public final class MarketStructureDetector {
    private static final int PIVOT_WINDOW = 2;

    public MarketStructureAnalysis analyze(List<Candle> candles) {
        if (candles == null || candles.size() < 30) {
            throw new IllegalArgumentException("São necessários pelo menos 30 candles para ler a estrutura.");
        }

        List<Swing> highs = new ArrayList<>();
        List<Swing> lows = new ArrayList<>();
        for (int i = PIVOT_WINDOW; i < candles.size() - PIVOT_WINDOW; i++) {
            Candle current = candles.get(i);
            if (isSwingHigh(candles, i)) highs.add(new Swing(i, current.getHigh()));
            if (isSwingLow(candles, i)) lows.add(new Swing(i, current.getLow()));
        }

        if (highs.size() < 2 || lows.size() < 2) {
            return new MarketStructureAnalysis(
                    MarketStructureAnalysis.Trend.UNDEFINED,
                    MarketStructureAnalysis.Event.NONE,
                    "--",
                    "--",
                    highest(candles),
                    lowest(candles),
                    false,
                    false,
                    20,
                    singleton("Poucos pivôs confirmados; aguarde mais formação de preço.")
            );
        }

        Swing previousHigh = highs.get(highs.size() - 2);
        Swing lastHigh = highs.get(highs.size() - 1);
        Swing previousLow = lows.get(lows.size() - 2);
        Swing lastLow = lows.get(lows.size() - 1);

        String highLabel = lastHigh.price > previousHigh.price ? "HH" : "LH";
        String lowLabel = lastLow.price > previousLow.price ? "HL" : "LL";

        MarketStructureAnalysis.Trend trend;
        if ("HH".equals(highLabel) && "HL".equals(lowLabel)) {
            trend = MarketStructureAnalysis.Trend.BULLISH;
        } else if ("LH".equals(highLabel) && "LL".equals(lowLabel)) {
            trend = MarketStructureAnalysis.Trend.BEARISH;
        } else {
            trend = MarketStructureAnalysis.Trend.RANGE;
        }

        Candle latest = candles.get(candles.size() - 1);
        MarketStructureAnalysis.Event event = detectEvent(
                trend,
                latest.getClose(),
                previousHigh.price,
                previousLow.price,
                lastHigh.price,
                lastLow.price
        );

        double averageRange = averageRange(candles, 20);
        double tolerance = Math.max(averageRange * 0.20, latest.getClose() * 0.0002);
        boolean equalHighs = Math.abs(lastHigh.price - previousHigh.price) <= tolerance;
        boolean equalLows = Math.abs(lastLow.price - previousLow.price) <= tolerance;

        List<String> evidence = new ArrayList<>();
        evidence.add("Topo recente classificado como " + highLabel + ".");
        evidence.add("Fundo recente classificado como " + lowLabel + ".");
        if (event != MarketStructureAnalysis.Event.NONE) {
            evidence.add("Evento estrutural detectado: " + event.name().replace('_', ' ') + ".");
        }
        if (equalHighs) evidence.add("Topos próximos indicam possível liquidez acima da resistência.");
        if (equalLows) evidence.add("Fundos próximos indicam possível liquidez abaixo do suporte.");

        int confidence = 55;
        if (trend != MarketStructureAnalysis.Trend.RANGE) confidence += 15;
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
                    || value < candles.get(index + offset).getHigh()) return false;
        }
        return true;
    }

    private boolean isSwingLow(List<Candle> candles, int index) {
        double value = candles.get(index).getLow();
        for (int offset = 1; offset <= PIVOT_WINDOW; offset++) {
            if (value >= candles.get(index - offset).getLow()
                    || value > candles.get(index + offset).getLow()) return false;
        }
        return true;
    }

    private MarketStructureAnalysis.Event detectEvent(
            MarketStructureAnalysis.Trend trend,
            double close,
            double previousHigh,
            double previousLow,
            double lastHigh,
            double lastLow
    ) {
        if (trend == MarketStructureAnalysis.Trend.BULLISH && close < lastLow) {
            return MarketStructureAnalysis.Event.CHOCH_BEARISH;
        }
        if (trend == MarketStructureAnalysis.Trend.BEARISH && close > lastHigh) {
            return MarketStructureAnalysis.Event.CHOCH_BULLISH;
        }
        if (close > Math.max(previousHigh, lastHigh)) {
            return MarketStructureAnalysis.Event.BOS_BULLISH;
        }
        if (close < Math.min(previousLow, lastLow)) {
            return MarketStructureAnalysis.Event.BOS_BEARISH;
        }
        return MarketStructureAnalysis.Event.NONE;
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

    private List<String> singleton(String value) {
        List<String> items = new ArrayList<>();
        items.add(value);
        return items;
    }

    private static final class Swing {
        private final int index;
        private final double price;

        private Swing(int index, double price) {
            this.index = index;
            this.price = price;
        }
    }
}
