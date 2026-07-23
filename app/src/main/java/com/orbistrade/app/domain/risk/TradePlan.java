package com.orbistrade.app.domain.risk;

import com.orbistrade.app.domain.strategy.StrategyDecision;

public final class TradePlan {
    private final StrategyDecision.Action action;
    private final double entryPrice;
    private final double stopLoss;
    private final double takeProfit;
    private final double riskAmount;
    private final double positionUnits;
    private final boolean executable;
    private final String note;

    public TradePlan(
            StrategyDecision.Action action,
            double entryPrice,
            double stopLoss,
            double takeProfit,
            double riskAmount,
            double positionUnits,
            boolean executable,
            String note
    ) {
        this.action = action;
        this.entryPrice = entryPrice;
        this.stopLoss = stopLoss;
        this.takeProfit = takeProfit;
        this.riskAmount = riskAmount;
        this.positionUnits = positionUnits;
        this.executable = executable;
        this.note = note;
    }

    public StrategyDecision.Action getAction() { return action; }
    public double getEntryPrice() { return entryPrice; }
    public double getStopLoss() { return stopLoss; }
    public double getTakeProfit() { return takeProfit; }
    public double getRiskAmount() { return riskAmount; }
    public double getPositionUnits() { return positionUnits; }
    public boolean isExecutable() { return executable; }
    public String getNote() { return note; }
}