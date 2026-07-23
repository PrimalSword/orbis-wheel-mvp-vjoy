package com.orbistrade.app.domain.strategy;

import com.orbistrade.app.domain.model.MarketRegime;
import com.orbistrade.app.domain.model.MarketSnapshot;

public final class RangeReversionStrategy implements TradingStrategy {
    @Override
    public String getName() {
        return "Range Reversion";
    }

    @Override
    public boolean supports(MarketRegime regime) {
        return regime == MarketRegime.RANGE;
    }

    @Override
    public StrategyDecision evaluate(MarketSnapshot snapshot) {
        if (snapshot.getRsi() <= 35) {
            return new StrategyDecision(getName(), StrategyDecision.Action.BUY, 70,
                    "Mercado lateral com RSI em região de sobrevenda.");
        }

        if (snapshot.getRsi() >= 65) {
            return new StrategyDecision(getName(), StrategyDecision.Action.SELL, 70,
                    "Mercado lateral com RSI em região de sobrecompra.");
        }

        return new StrategyDecision(getName(), StrategyDecision.Action.WAIT, 45,
                "Mercado lateral sem extremo estatístico.");
    }
}
