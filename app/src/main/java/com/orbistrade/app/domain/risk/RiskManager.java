package com.orbistrade.app.domain.risk;

import com.orbistrade.app.domain.model.MarketSnapshot;
import com.orbistrade.app.domain.strategy.StrategyDecision;

public final class RiskManager {

    public TradePlan buildPlan(
            MarketSnapshot market,
            StrategyDecision decision,
            RiskProfile profile
    ) {
        double riskAmount = profile.getAccountBalance() * (profile.getRiskPercent() / 100.0);

        if (decision.getAction() == StrategyDecision.Action.WAIT) {
            return new TradePlan(
                    decision.getAction(), market.getPrice(), market.getPrice(), market.getPrice(),
                    0, 0, false, "Sem posição: a estratégia recomendou aguardar."
            );
        }

        if (decision.getConfidence() < 60) {
            return new TradePlan(
                    decision.getAction(), market.getPrice(), market.getPrice(), market.getPrice(),
                    0, 0, false, "Sinal bloqueado: confiança abaixo de 60%."
            );
        }

        double stopDistance = market.getPrice()
                * (market.getAtrPercent() / 100.0)
                * profile.getAtrStopMultiplier();

        if (stopDistance <= 0) {
            return new TradePlan(
                    decision.getAction(), market.getPrice(), market.getPrice(), market.getPrice(),
                    0, 0, false, "Sinal bloqueado: volatilidade inválida."
            );
        }

        boolean buy = decision.getAction() == StrategyDecision.Action.BUY;
        double stopLoss = buy
                ? market.getPrice() - stopDistance
                : market.getPrice() + stopDistance;
        double takeProfitDistance = stopDistance * profile.getRewardRiskRatio();
        double takeProfit = buy
                ? market.getPrice() + takeProfitDistance
                : market.getPrice() - takeProfitDistance;
        double positionUnits = riskAmount / stopDistance;

        return new TradePlan(
                decision.getAction(),
                market.getPrice(),
                stopLoss,
                takeProfit,
                riskAmount,
                positionUnits,
                true,
                "Plano calculado por volatilidade (ATR), sem aumentar o risco definido."
        );
    }
}