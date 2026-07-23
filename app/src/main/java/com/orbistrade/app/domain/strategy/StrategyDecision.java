package com.orbistrade.app.domain.strategy;

public final class StrategyDecision {
    public enum Action {
        BUY,
        SELL,
        WAIT
    }

    private final String strategyName;
    private final Action action;
    private final int confidence;
    private final String rationale;

    public StrategyDecision(String strategyName, Action action, int confidence, String rationale) {
        this.strategyName = strategyName;
        this.action = action;
        this.confidence = Math.max(0, Math.min(100, confidence));
        this.rationale = rationale;
    }

    public String getStrategyName() {
        return strategyName;
    }

    public Action getAction() {
        return action;
    }

    public int getConfidence() {
        return confidence;
    }

    public String getRationale() {
        return rationale;
    }
}
