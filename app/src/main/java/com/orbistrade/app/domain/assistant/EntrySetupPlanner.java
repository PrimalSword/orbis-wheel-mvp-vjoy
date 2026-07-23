package com.orbistrade.app.domain.assistant;

import com.orbistrade.app.domain.model.MarketSnapshot;
import com.orbistrade.app.domain.risk.TradePlan;
import com.orbistrade.app.domain.strategy.StrategyDecision;

public final class EntrySetupPlanner {

    public EntrySetup build(
            MarketSnapshot snapshot,
            StrategyDecision decision,
            TradePlan plan
    ) {
        if (!plan.isExecutable() || decision.getAction() == StrategyDecision.Action.WAIT) {
            return new EntrySetup(
                    EntrySetup.Status.BLOCKED,
                    decision.getStrategyName(),
                    0,
                    snapshot.getPrice(),
                    snapshot.getPrice(),
                    "Nenhuma entrada deve ser considerada agora.",
                    "O plano só será criado quando estratégia, confiança e risco estiverem alinhados."
            );
        }

        double atrDistance = snapshot.getPrice() * (snapshot.getAtrPercent() / 100.0);
        int windowMinutes = snapshot.getAtrPercent() >= 1.2 ? 15 : 30;

        if (decision.getAction() == StrategyDecision.Action.BUY) {
            double pullbackTrigger = Math.max(snapshot.getEma20(), snapshot.getPrice() - atrDistance * 0.35);
            return new EntrySetup(
                    EntrySetup.Status.WAIT_TRIGGER,
                    decision.getStrategyName(),
                    windowMinutes,
                    pullbackTrigger,
                    plan.getStopLoss(),
                    "Aguardar o preço tocar a zona e um candle de 5 minutos fechar acima dela, sem RSI acima de 70.",
                    "A janela é uma estimativa, não uma promessa. Se o preço não confirmar, não entrar."
            );
        }

        double pullbackTrigger = Math.min(snapshot.getEma20(), snapshot.getPrice() + atrDistance * 0.35);
        return new EntrySetup(
                EntrySetup.Status.WAIT_TRIGGER,
                decision.getStrategyName(),
                windowMinutes,
                pullbackTrigger,
                plan.getStopLoss(),
                "Aguardar o preço tocar a zona e um candle de 5 minutos fechar abaixo dela, sem RSI abaixo de 30.",
                "A janela é uma estimativa, não uma promessa. Se o preço não confirmar, não entrar."
        );
    }
}