package com.orbistrade.app.domain.assistant;

import com.orbistrade.app.domain.model.MarketRegime;
import com.orbistrade.app.domain.model.MarketSnapshot;
import com.orbistrade.app.domain.risk.TradePlan;
import com.orbistrade.app.domain.strategy.StrategyDecision;

import java.util.ArrayList;
import java.util.List;

public final class BiasGuard {

    public ConfirmationReview review(
            TradeThesis thesis,
            MarketSnapshot snapshot,
            MarketRegime regime,
            StrategyDecision decision,
            TradePlan plan
    ) {
        List<String> supporting = new ArrayList<>();
        List<String> opposing = new ArrayList<>();

        if (thesis == TradeThesis.NEUTRAL) {
            opposing.add("Nenhuma hipótese direcional foi informada para ser validada.");
            return new ConfirmationReview(
                    ConfirmationReview.Verdict.INCONCLUSIVE,
                    50,
                    supporting,
                    opposing,
                    "Defina uma hipótese de compra ou venda antes de usar a análise como segunda opinião."
            );
        }

        StrategyDecision.Action expectedAction = thesis == TradeThesis.BUY
                ? StrategyDecision.Action.BUY
                : StrategyDecision.Action.SELL;

        if (decision.getAction() == expectedAction) {
            supporting.add("A estratégia selecionada aponta na mesma direção da sua hipótese.");
        } else if (decision.getAction() == StrategyDecision.Action.WAIT) {
            opposing.add("O motor não encontrou qualidade suficiente para uma entrada agora.");
        } else {
            opposing.add("A estratégia selecionada aponta na direção oposta à sua hipótese.");
        }

        if (decision.getConfidence() >= 70) {
            supporting.add("A confiança técnica do sinal está acima de 70%.");
        } else if (decision.getConfidence() < 60) {
            opposing.add("A confiança técnica está abaixo do limite mínimo de segurança.");
        } else {
            opposing.add("A confiança técnica é apenas moderada.");
        }

        boolean bullishStructure = snapshot.getPrice() > snapshot.getEma20()
                && snapshot.getEma20() > snapshot.getEma200();
        boolean bearishStructure = snapshot.getPrice() < snapshot.getEma20()
                && snapshot.getEma20() < snapshot.getEma200();

        if ((thesis == TradeThesis.BUY && bullishStructure)
                || (thesis == TradeThesis.SELL && bearishStructure)) {
            supporting.add("Preço e médias móveis estão alinhados com a hipótese.");
        } else {
            opposing.add("Preço e médias móveis não apresentam alinhamento completo com a hipótese.");
        }

        if (thesis == TradeThesis.BUY && snapshot.getRsi() >= 70) {
            opposing.add("RSI em sobrecompra aumenta o risco de entrada tardia.");
        } else if (thesis == TradeThesis.SELL && snapshot.getRsi() <= 30) {
            opposing.add("RSI em sobrevenda aumenta o risco de vender tarde.");
        } else {
            supporting.add("O RSI não está em extremo contrário à entrada proposta.");
        }

        if (snapshot.getAtrPercent() > 2.0) {
            opposing.add("A volatilidade está elevada e pode aumentar slippage e distância do stop.");
        } else {
            supporting.add("A volatilidade está dentro do limite operacional inicial.");
        }

        if (plan.isExecutable()) {
            supporting.add("A gestão de risco conseguiu construir um plano executável.");
        } else {
            opposing.add("A gestão de risco bloqueou a operação: " + plan.getNote());
        }

        int score = 50 + supporting.size() * 10 - opposing.size() * 12;
        score = Math.max(0, Math.min(100, score));

        ConfirmationReview.Verdict verdict;
        String conclusion;

        if (!plan.isExecutable()) {
            verdict = ConfirmationReview.Verdict.BLOCKED;
            conclusion = "Operação bloqueada. Não use a análise para justificar uma entrada fora do plano de risco.";
        } else if (decision.getAction() == expectedAction && score >= 70) {
            verdict = ConfirmationReview.Verdict.CONFIRMED;
            conclusion = "A hipótese possui confluência técnica, mas continua sendo um cenário probabilístico, não uma garantia.";
        } else if (decision.getAction() != StrategyDecision.Action.WAIT
                && decision.getAction() != expectedAction) {
            verdict = ConfirmationReview.Verdict.CONFLICT;
            conclusion = "A leitura objetiva conflita com sua hipótese. Reavalie a entrada em vez de procurar confirmação.";
        } else {
            verdict = ConfirmationReview.Verdict.INCONCLUSIVE;
            conclusion = "A evidência ainda não é forte o bastante. Esperar por melhor confluência é uma decisão válida.";
        }

        return new ConfirmationReview(verdict, score, supporting, opposing, conclusion);
    }
}
