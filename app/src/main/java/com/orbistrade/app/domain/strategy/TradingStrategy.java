package com.orbistrade.app.domain.strategy;

import com.orbistrade.app.domain.model.MarketRegime;
import com.orbistrade.app.domain.model.MarketSnapshot;

public interface TradingStrategy {
    String getName();

    boolean supports(MarketRegime regime);

    StrategyDecision evaluate(MarketSnapshot snapshot);
}
