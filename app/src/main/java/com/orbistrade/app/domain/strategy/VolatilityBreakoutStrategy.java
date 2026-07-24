package com.orbistrade.app.domain.strategy;

import com.orbistrade.app.domain.model.MarketRegime;
import com.orbistrade.app.domain.model.MarketSnapshot;

public final class VolatilityBreakoutStrategy implements TradingStrategy {
    @Override
    public String getName() {
        return "Volatility Breakout";
    }

    @Override
    public boolean supports(MarketRegime regime) {
        return regime == MarketRegime.HIGH_VOLATILITY
                || regime == MarketRegime.STRONG_UPTREND
                || regime == MarketRegime.STRONG_DOWNTREND;
    }

    @Override
    public StrategyDecision evaluate(MarketSnapshot snapshot) {
        boolean elevatedVolatility = snapshot.getAtrPercent() >= 0.35;

        if (elevatedVolatility
                && snapshot.getPrice() > snapshot.getEma20()
                && snapshot.getEma20() > snapshot.getEma200()
                && snapshot.getRsi() >= 55
                && snapshot.getRsi() <= 72) {
            int confidence = snapshot.getAtrPercent() >= 0.70 ? 84 : 76;
            return new StrategyDecision(getName(), StrategyDecision.Action.BUY, confidence,
                    "Expansão de volatilidade com preço e médias alinhados para cima." );
        }

        if (elevatedVolatility
                && snapshot.getPrice() < snapshot.getEma20()
                && snapshot.getEma20() < snapshot.getEma200()
                && snapshot.getRsi() <= 45
                && snapshot.getRsi() >= 28) {
            int confidence = snapshot.getAtrPercent() >= 0.70 ? 84 : 76;
            return new StrategyDecision(getName(), StrategyDecision.Action.SELL, confidence,
                    "Expansão de volatilidade com preço e médias alinhados para baixo." );
        }

        return new StrategyDecision(getName(), StrategyDecision.Action.WAIT, 32,
                "Volatilidade sem rompimento direcional confirmado." );
    }
}
