package com.orbistrade.app.domain.market;

import com.orbistrade.app.data.market.Candle;
import com.orbistrade.app.domain.indicator.TechnicalIndicatorCalculator;
import com.orbistrade.app.domain.model.MarketSnapshot;

import java.util.List;

public final class MarketSnapshotFactory {
    private final TechnicalIndicatorCalculator indicators;

    public MarketSnapshotFactory() {
        this(new TechnicalIndicatorCalculator());
    }

    public MarketSnapshotFactory(TechnicalIndicatorCalculator indicators) {
        this.indicators = indicators;
    }

    public MarketSnapshot create(String symbol, List<Candle> candles) {
        if (candles == null || candles.size() < 200) {
            throw new IllegalArgumentException("São necessários ao menos 200 candles.");
        }

        double price = candles.get(candles.size() - 1).getClose();
        return new MarketSnapshot(
                symbol,
                price,
                indicators.ema(candles, 20),
                indicators.ema(candles, 200),
                indicators.rsi(candles, 14),
                indicators.atrPercent(candles, 14)
        );
    }
}