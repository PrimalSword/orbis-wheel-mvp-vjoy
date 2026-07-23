package com.orbistrade.app.domain.assistant;

import com.orbistrade.app.domain.model.MarketSnapshot;
import com.orbistrade.app.domain.risk.TradePlan;
import com.orbistrade.app.domain.strategy.StrategyDecision;

import java.util.ArrayList;
import java.util.List;

public final class SetupScorer {

    public SetupAssessment score(
            MarketSnapshot context15m,
            MarketSnapshot trigger5m,
            MultiTimeframeAnalysis multiTimeframe,
            StrategyDecision decision,
            TradePlan plan
    ) {
        List<String> strengths = new ArrayList<>();
        List<String> weaknesses = new ArrayList<>();

        int trend = scoreTrend(context15m, decision, strengths, weaknesses);
        int timeframes = scoreTimeframes(multiTimeframe, strengths, weaknesses);
        int momentum = scoreMomentum(trigger5m, decision, strengths, weaknesses);
        int volatility = scoreVolatility(trigger5m, strengths, weaknesses);
        int risk = scoreRisk(plan, strengths, weaknesses);

        int total = trend + timeframes + momentum + volatility + risk;
        return new SetupAssessment(
                total,
                grade(total),
                trend,
                timeframes,
                momentum,
                volatility,
                risk,
                strengths,
                weaknesses
        );
    }

    private int scoreTrend(
            MarketSnapshot snapshot,
            StrategyDecision decision,
            List<String> strengths,
            List<String> weaknesses
    ) {
        boolean bullish = snapshot.getPrice() > snapshot.getEma20()
                && snapshot.getEma20() > snapshot.getEma200();
        boolean bearish = snapshot.getPrice() < snapshot.getEma20()
                && snapshot.getEma20() < snapshot.getEma200();
        boolean aligned = (decision.getAction() == StrategyDecision.Action.BUY && bullish)
                || (decision.getAction() == StrategyDecision.Action.SELL && bearish);

        if (aligned) {
            strengths.add("Tendência de 15m alinhada com preço, EMA20 e EMA200.");
            return 20;
        }
        if (decision.getAction() == StrategyDecision.Action.WAIT) {
            weaknesses.add("Ainda não existe direção operacional válida.");
            return 5;
        }
        weaknesses.add("A direção proposta não está totalmente alinhada às médias do 15m.");
        return 8;
    }

    private int scoreTimeframes(
            MultiTimeframeAnalysis analysis,
            List<String> strengths,
            List<String> weaknesses
    ) {
        if (analysis.getStatus() == MultiTimeframeAnalysis.Status.ALIGNED) {
            strengths.add("Os gráficos de 15m e 5m apontam para a mesma direção.");
            return 20;
        }
        if (analysis.getStatus() == MultiTimeframeAnalysis.Status.CONFLICT) {
            weaknesses.add("Existe conflito entre contexto e gatilho.");
            return 0;
        }
        weaknesses.add("O contexto existe, mas o gatilho ainda não confirmou.");
        return 6;
    }

    private int scoreMomentum(
            MarketSnapshot snapshot,
            StrategyDecision decision,
            List<String> strengths,
            List<String> weaknesses
    ) {
        double rsi = snapshot.getRsi();
        boolean buyHealthy = decision.getAction() == StrategyDecision.Action.BUY && rsi >= 45 && rsi <= 68;
        boolean sellHealthy = decision.getAction() == StrategyDecision.Action.SELL && rsi >= 32 && rsi <= 55;

        if (buyHealthy || sellHealthy) {
            strengths.add("RSI de 5m confirma impulso sem extremo evidente.");
            return 20;
        }
        if (rsi > 70 || rsi < 30) {
            weaknesses.add("RSI está em região extrema; perseguir o preço aumenta o risco.");
            return 4;
        }
        weaknesses.add("Momentum de 5m ainda é pouco convincente.");
        return 10;
    }

    private int scoreVolatility(
            MarketSnapshot snapshot,
            List<String> strengths,
            List<String> weaknesses
    ) {
        double atr = snapshot.getAtrPercent();
        if (atr >= 0.20 && atr <= 1.50) {
            strengths.add("Volatilidade está em uma faixa utilizável para setup intraday.");
            return 20;
        }
        if (atr < 0.20) {
            weaknesses.add("Volatilidade baixa: o preço pode não desenvolver o movimento no prazo esperado.");
            return 8;
        }
        weaknesses.add("Volatilidade elevada: risco de ruído, slippage e stop prematuro.");
        return 6;
    }

    private int scoreRisk(
            TradePlan plan,
            List<String> strengths,
            List<String> weaknesses
    ) {
        if (plan.isExecutable()) {
            strengths.add("Plano possui entrada, invalidação, alvo e risco financeiro definidos.");
            return 20;
        }
        weaknesses.add("O gerenciamento de risco bloqueou a execução.");
        return 0;
    }

    private String grade(int score) {
        if (score >= 85) return "A";
        if (score >= 70) return "B";
        if (score >= 55) return "C";
        if (score >= 40) return "D";
        return "F";
    }
}