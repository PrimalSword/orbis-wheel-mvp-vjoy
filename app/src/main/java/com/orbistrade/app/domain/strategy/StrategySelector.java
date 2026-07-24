package com.orbistrade.app.domain.strategy;

import com.orbistrade.app.domain.model.MarketRegime;
import com.orbistrade.app.domain.model.MarketSnapshot;

import java.util.Arrays;
import java.util.List;

public final class StrategySelector {
    private final MarketRegimeDetector regimeDetector;
    private final List<TradingStrategy> strategies;

    public StrategySelector() {
        this.regimeDetector = new MarketRegimeDetector();
        this.strategies = Arrays.asList(
                new TrendFollowingStrategy(),
                new TrendPullbackStrategy(),
                new VolatilityBreakoutStrategy(),
                new RangeReversionStrategy()
        );
    }

    public SelectionResult analyze(MarketSnapshot snapshot) {
        MarketRegime regime = regimeDetector.detect(snapshot);
        StrategyDecision bestActionable = null;
        StrategyDecision bestWait = null;

        for (TradingStrategy strategy : strategies) {
            if (!strategy.supports(regime)) continue;

            StrategyDecision candidate = strategy.evaluate(snapshot);
            if (candidate.getAction() == StrategyDecision.Action.WAIT) {
                if (bestWait == null || candidate.getConfidence() > bestWait.getConfidence()) {
                    bestWait = candidate;
                }
            } else if (bestActionable == null || candidate.getConfidence() > bestActionable.getConfidence()) {
                bestActionable = candidate;
            }
        }

        if (bestActionable != null) {
            return new SelectionResult(regime, bestActionable);
        }

        if (bestWait != null) {
            return new SelectionResult(regime, bestWait);
        }

        StrategyDecision fallback = new StrategyDecision(
                "Nenhuma estratégia",
                StrategyDecision.Action.WAIT,
                20,
                "O regime atual ainda não possui estratégia aprovada."
        );
        return new SelectionResult(regime, fallback);
    }

    public static final class SelectionResult {
        private final MarketRegime regime;
        private final StrategyDecision decision;

        public SelectionResult(MarketRegime regime, StrategyDecision decision) {
            this.regime = regime;
            this.decision = decision;
        }

        public MarketRegime getRegime() {
            return regime;
        }

        public StrategyDecision getDecision() {
            return decision;
        }
    }
}
