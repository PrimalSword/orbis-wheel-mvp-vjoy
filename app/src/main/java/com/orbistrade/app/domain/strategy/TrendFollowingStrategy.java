package com.orbistrade.app.domain.strategy;

import com.orbistrade.app.domain.model.MarketRegime;
import com.orbistrade.app.domain.model.MarketSnapshot;

public final class TrendFollowingStrategy implements TradingStrategy {
    @Override
    public String getName() {
        return "Trend Following";
    }

    @Override
    public boolean supports(MarketRegime regime) {
        return regime == MarketRegime.STRONG_UPTREND || regime == MarketRegime.STRONG_DOWNTREND;
    }

    @Override
    public StrategyDecision evaluate(MarketSnapshot snapshot) {
        if (snapshot.getEma20() > snapshot.getEma200() && snapshot.getPrice() > snapshot.getEma20()) {
            int confidence = snapshot.getRsi() >= 50 && snapshot.getRsi() <= 70 ? 78 : 62;
            return new StrategyDecision(getName(), StrategyDecision.Action.BUY, confidence,
                    "Preço acima da EMA20 e EMA20 acima da EMA200.");
        }

        if (snapshot.getEma20() < snapshot.getEma200() && snapshot.getPrice() < snapshot.getEma20()) {
            int confidence = snapshot.getRsi() <= 50 && snapshot.getRsi() >= 30 ? 78 : 62;
            return new StrategyDecision(getName(), StrategyDecision.Action.SELL, confidence,
                    "Preço abaixo da EMA20 e EMA20 abaixo da EMA200.");
        }

        return new StrategyDecision(getName(), StrategyDecision.Action.WAIT, 35,
                "Tendência sem alinhamento suficiente.");
    }
}
