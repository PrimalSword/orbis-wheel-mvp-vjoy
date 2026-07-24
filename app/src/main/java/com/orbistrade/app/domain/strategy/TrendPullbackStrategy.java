package com.orbistrade.app.domain.strategy;

import com.orbistrade.app.domain.model.MarketRegime;
import com.orbistrade.app.domain.model.MarketSnapshot;

public final class TrendPullbackStrategy implements TradingStrategy {
    @Override
    public String getName() {
        return "Trend Pullback";
    }

    @Override
    public boolean supports(MarketRegime regime) {
        return regime == MarketRegime.STRONG_UPTREND || regime == MarketRegime.STRONG_DOWNTREND;
    }

    @Override
    public StrategyDecision evaluate(MarketSnapshot snapshot) {
        double emaDistancePercent = Math.abs(snapshot.getPrice() - snapshot.getEma20())
                / Math.max(Math.abs(snapshot.getPrice()), 0.0000001) * 100.0;
        double tolerance = Math.max(0.10, snapshot.getAtrPercent() * 0.45);
        boolean nearEma20 = emaDistancePercent <= tolerance;

        if (snapshot.getEma20() > snapshot.getEma200()
                && nearEma20
                && snapshot.getRsi() >= 42
                && snapshot.getRsi() <= 58) {
            int confidence = snapshot.getPrice() >= snapshot.getEma20() ? 82 : 74;
            return new StrategyDecision(getName(), StrategyDecision.Action.BUY, confidence,
                    "Tendência de alta com recuo controlado próximo da EMA20 e RSI neutro." );
        }

        if (snapshot.getEma20() < snapshot.getEma200()
                && nearEma20
                && snapshot.getRsi() >= 42
                && snapshot.getRsi() <= 58) {
            int confidence = snapshot.getPrice() <= snapshot.getEma20() ? 82 : 74;
            return new StrategyDecision(getName(), StrategyDecision.Action.SELL, confidence,
                    "Tendência de baixa com repique controlado próximo da EMA20 e RSI neutro." );
        }

        return new StrategyDecision(getName(), StrategyDecision.Action.WAIT, 40,
                "Sem pullback limpo próximo da EMA20." );
    }
}
