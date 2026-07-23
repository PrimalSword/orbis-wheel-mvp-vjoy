package com.orbistrade.app.domain.strategy;

import com.orbistrade.app.domain.model.MarketRegime;
import com.orbistrade.app.domain.model.MarketSnapshot;

public final class MarketRegimeDetector {

    public MarketRegime detect(MarketSnapshot snapshot) {
        if (snapshot.getAtrPercent() >= 2.5) {
            return MarketRegime.HIGH_VOLATILITY;
        }

        double emaDistancePercent = Math.abs(snapshot.getEma20() - snapshot.getEma200())
                / Math.max(snapshot.getPrice(), 0.000001) * 100.0;

        if (emaDistancePercent < 0.25) {
            return MarketRegime.RANGE;
        }

        if (snapshot.getEma20() > snapshot.getEma200() && snapshot.getPrice() > snapshot.getEma20()) {
            return MarketRegime.STRONG_UPTREND;
        }

        if (snapshot.getEma20() < snapshot.getEma200() && snapshot.getPrice() < snapshot.getEma20()) {
            return MarketRegime.STRONG_DOWNTREND;
        }

        return MarketRegime.UNDEFINED;
    }
}
