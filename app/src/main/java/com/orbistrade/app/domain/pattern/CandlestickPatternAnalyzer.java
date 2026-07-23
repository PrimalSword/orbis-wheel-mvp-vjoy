package com.orbistrade.app.domain.pattern;

import com.orbistrade.app.data.market.Candle;

import java.util.ArrayList;
import java.util.List;

public final class CandlestickPatternAnalyzer {

    public CandlestickPatternAnalysis analyze(List<Candle> candles) {
        if (candles == null || candles.size() < 3) {
            throw new IllegalArgumentException("São necessários ao menos 3 candles para analisar padrões.");
        }

        Candle current = candles.get(candles.size() - 1);
        Candle previous = candles.get(candles.size() - 2);
        Candle third = candles.get(candles.size() - 3);

        List<String> patterns = new ArrayList<>();
        List<String> evidence = new ArrayList<>();
        int bullish = 0;
        int bearish = 0;

        if (isDoji(current)) {
            patterns.add("Doji");
            evidence.add("Corpo muito pequeno em relação à amplitude: indecisão e necessidade de confirmação.");
        }
        if (isBullishPinBar(current)) {
            patterns.add("Pin bar altista");
            evidence.add("Sombra inferior longa sugere rejeição de preços mais baixos.");
            bullish += 2;
        }
        if (isBearishPinBar(current)) {
            patterns.add("Pin bar baixista");
            evidence.add("Sombra superior longa sugere rejeição de preços mais altos.");
            bearish += 2;
        }
        if (isBullishEngulfing(previous, current)) {
            patterns.add("Engolfo altista");
            evidence.add("O corpo comprador engolfou o corpo vendedor anterior.");
            bullish += 3;
        }
        if (isBearishEngulfing(previous, current)) {
            patterns.add("Engolfo baixista");
            evidence.add("O corpo vendedor engolfou o corpo comprador anterior.");
            bearish += 3;
        }
        if (isInsideBar(previous, current)) {
            patterns.add("Inside bar");
            evidence.add("Compressão dentro da amplitude anterior; aguarde rompimento confirmado.");
        }
        if (isMorningStar(third, previous, current)) {
            patterns.add("Morning star");
            evidence.add("Sequência de três candles compatível com reversão altista.");
            bullish += 4;
        }
        if (isEveningStar(third, previous, current)) {
            patterns.add("Evening star");
            evidence.add("Sequência de três candles compatível com reversão baixista.");
            bearish += 4;
        }

        CandlestickPatternAnalysis.Bias bias;
        if (bullish > bearish) bias = CandlestickPatternAnalysis.Bias.BULLISH;
        else if (bearish > bullish) bias = CandlestickPatternAnalysis.Bias.BEARISH;
        else bias = CandlestickPatternAnalysis.Bias.NEUTRAL;

        int strength = Math.max(bullish, bearish);
        int confidence = patterns.isEmpty() ? 20 : Math.min(90, 40 + strength * 10);
        if (patterns.isEmpty()) evidence.add("Nenhum padrão clássico forte foi confirmado nos candles mais recentes.");

        return new CandlestickPatternAnalysis(bias, confidence, patterns, evidence);
    }

    private boolean isDoji(Candle candle) {
        return body(candle) <= range(candle) * 0.12;
    }

    private boolean isBullishPinBar(Candle candle) {
        double body = body(candle);
        return lowerWick(candle) >= body * 2.2
                && upperWick(candle) <= Math.max(body, range(candle) * 0.18)
                && candle.getClose() >= candle.getOpen();
    }

    private boolean isBearishPinBar(Candle candle) {
        double body = body(candle);
        return upperWick(candle) >= body * 2.2
                && lowerWick(candle) <= Math.max(body, range(candle) * 0.18)
                && candle.getClose() <= candle.getOpen();
    }

    private boolean isBullishEngulfing(Candle previous, Candle current) {
        return previous.getClose() < previous.getOpen()
                && current.getClose() > current.getOpen()
                && current.getOpen() <= previous.getClose()
                && current.getClose() >= previous.getOpen();
    }

    private boolean isBearishEngulfing(Candle previous, Candle current) {
        return previous.getClose() > previous.getOpen()
                && current.getClose() < current.getOpen()
                && current.getOpen() >= previous.getClose()
                && current.getClose() <= previous.getOpen();
    }

    private boolean isInsideBar(Candle previous, Candle current) {
        return current.getHigh() < previous.getHigh() && current.getLow() > previous.getLow();
    }

    private boolean isMorningStar(Candle first, Candle middle, Candle last) {
        double midpoint = (first.getOpen() + first.getClose()) / 2.0;
        return first.getClose() < first.getOpen()
                && body(middle) <= body(first) * 0.45
                && last.getClose() > last.getOpen()
                && last.getClose() > midpoint;
    }

    private boolean isEveningStar(Candle first, Candle middle, Candle last) {
        double midpoint = (first.getOpen() + first.getClose()) / 2.0;
        return first.getClose() > first.getOpen()
                && body(middle) <= body(first) * 0.45
                && last.getClose() < last.getOpen()
                && last.getClose() < midpoint;
    }

    private double body(Candle candle) {
        return Math.abs(candle.getClose() - candle.getOpen());
    }

    private double range(Candle candle) {
        return Math.max(0.0000001, candle.getHigh() - candle.getLow());
    }

    private double upperWick(Candle candle) {
        return candle.getHigh() - Math.max(candle.getOpen(), candle.getClose());
    }

    private double lowerWick(Candle candle) {
        return Math.min(candle.getOpen(), candle.getClose()) - candle.getLow();
    }
}
