package com.orbistrade.app.domain.assistant;

import com.orbistrade.app.domain.model.MarketRegime;
import com.orbistrade.app.domain.model.MarketSnapshot;
import com.orbistrade.app.domain.strategy.StrategyDecision;

public final class MultiTimeframeAnalyzer {

    public MultiTimeframeAnalysis analyze(
            MarketSnapshot context15m,
            MarketRegime contextRegime,
            StrategyDecision contextDecision,
            MarketSnapshot trigger5m,
            MarketRegime triggerRegime,
            StrategyDecision triggerDecision
    ) {
        String contextSummary = "15m: " + contextRegime.name()
                + " | " + contextDecision.getAction().name()
                + " | confiança " + contextDecision.getConfidence() + "%";
        String triggerSummary = "5m: " + triggerRegime.name()
                + " | " + triggerDecision.getAction().name()
                + " | confiança " + triggerDecision.getConfidence() + "%";

        if (contextDecision.getAction() == StrategyDecision.Action.WAIT) {
            return new MultiTimeframeAnalysis(
                    MultiTimeframeAnalysis.Status.BLOCKED,
                    20,
                    contextSummary,
                    triggerSummary,
                    "O gráfico de 15 minutos não definiu direção. Não procurar entrada no 5 minutos."
            );
        }

        if (triggerDecision.getAction() == StrategyDecision.Action.WAIT) {
            int score = Math.min(59, contextDecision.getConfidence());
            return new MultiTimeframeAnalysis(
                    MultiTimeframeAnalysis.Status.BLOCKED,
                    score,
                    contextSummary,
                    triggerSummary,
                    "A direção existe no 15 minutos, mas o 5 minutos ainda não confirmou o gatilho. Aguardar."
            );
        }

        if (contextDecision.getAction() != triggerDecision.getAction()) {
            return new MultiTimeframeAnalysis(
                    MultiTimeframeAnalysis.Status.CONFLICT,
                    30,
                    contextSummary,
                    triggerSummary,
                    "Os tempos gráficos discordam. Não entrar até o 5 minutos voltar a alinhar com o 15 minutos."
            );
        }

        int score = (contextDecision.getConfidence() + triggerDecision.getConfidence()) / 2;
        if (contextRegime == triggerRegime) {
            score = Math.min(100, score + 10);
        }
        if (context15m.getPrice() > context15m.getEma200()
                && trigger5m.getPrice() > trigger5m.getEma200()
                && contextDecision.getAction() == StrategyDecision.Action.BUY) {
            score = Math.min(100, score + 5);
        }
        if (context15m.getPrice() < context15m.getEma200()
                && trigger5m.getPrice() < trigger5m.getEma200()
                && contextDecision.getAction() == StrategyDecision.Action.SELL) {
            score = Math.min(100, score + 5);
        }

        return new MultiTimeframeAnalysis(
                MultiTimeframeAnalysis.Status.ALIGNED,
                score,
                contextSummary,
                triggerSummary,
                "Direção confirmada no 15 minutos e gatilho compatível no 5 minutos. Ainda aguarde o fechamento do candle na zona indicada."
        );
    }
}
