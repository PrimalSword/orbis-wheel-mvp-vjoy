package com.orbistrade.app.domain.assistant;

import com.orbistrade.app.domain.model.MarketSnapshot;
import com.orbistrade.app.domain.risk.TradePlan;
import com.orbistrade.app.domain.strategy.StrategyDecision;

public final class MentorExplainer {

    public String explain(
            MarketSnapshot context15m,
            MarketSnapshot trigger5m,
            MultiTimeframeAnalysis multiTimeframe,
            StrategyDecision decision,
            TradePlan plan,
            SetupAssessment assessment
    ) {
        StringBuilder text = new StringBuilder();
        text.append("O que observar\n");

        if (multiTimeframe.getStatus() != MultiTimeframeAnalysis.Status.ALIGNED) {
            text.append("Ainda não há uma entrada válida. O gráfico de 15 minutos define o contexto, mas o gráfico de 5 minutos precisa confirmar a mesma direção. ");
            text.append("Enquanto isso não acontecer, esperar é uma decisão técnica, não falta de oportunidade.");
            return text.toString();
        }

        if (decision.getAction() == StrategyDecision.Action.BUY) {
            text.append("O contexto de 15 minutos favorece compradores. No gráfico de 5 minutos, não compre apenas porque o preço caiu até uma média: espere o candle reagir e fechar confirmando força compradora. ");
        } else if (decision.getAction() == StrategyDecision.Action.SELL) {
            text.append("O contexto de 15 minutos favorece vendedores. No gráfico de 5 minutos, não venda apenas porque o preço subiu até uma média: espere rejeição e fechamento confirmando força vendedora. ");
        } else {
            text.append("A leitura atual não oferece direção suficiente. Preserve capital e aguarde uma estrutura mais clara. ");
        }

        text.append("A nota ").append(assessment.getGrade())
                .append(" (").append(assessment.getTotalScore()).append("/100) ")
                .append("mede a qualidade das condições atuais, não a certeza do resultado. ");

        if (plan.isExecutable()) {
            text.append("O stop representa o ponto em que a hipótese deixa de fazer sentido; ele não deve ser afastado para evitar uma perda. ");
        }

        if (trigger5m.getRsi() > 70 || trigger5m.getRsi() < 30) {
            text.append("O RSI está extremo, então evite perseguir um movimento que já pode estar esticado. ");
        }

        text.append("Regra prática: contexto primeiro, gatilho depois, risco sempre definido antes da entrada.");
        return text.toString();
    }
}