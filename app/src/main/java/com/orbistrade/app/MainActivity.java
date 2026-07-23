package com.orbistrade.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.orbistrade.app.data.market.Candle;
import com.orbistrade.app.data.market.DemoMarketDataProvider;
import com.orbistrade.app.data.market.MarketDataProvider;
import com.orbistrade.app.data.market.TwelveDataMarketDataProvider;
import com.orbistrade.app.data.settings.ApiKeyStore;
import com.orbistrade.app.domain.assistant.BiasGuard;
import com.orbistrade.app.domain.assistant.ConfirmationReview;
import com.orbistrade.app.domain.assistant.EntrySetup;
import com.orbistrade.app.domain.assistant.EntrySetupPlanner;
import com.orbistrade.app.domain.assistant.MentorExplainer;
import com.orbistrade.app.domain.assistant.MultiTimeframeAnalysis;
import com.orbistrade.app.domain.assistant.MultiTimeframeAnalyzer;
import com.orbistrade.app.domain.assistant.SetupAssessment;
import com.orbistrade.app.domain.assistant.SetupScorer;
import com.orbistrade.app.domain.assistant.TradeThesis;
import com.orbistrade.app.domain.market.MarketSnapshotFactory;
import com.orbistrade.app.domain.model.MarketSnapshot;
import com.orbistrade.app.domain.pattern.CandlestickPatternAnalysis;
import com.orbistrade.app.domain.pattern.CandlestickPatternAnalyzer;
import com.orbistrade.app.domain.risk.RiskManager;
import com.orbistrade.app.domain.risk.RiskProfile;
import com.orbistrade.app.domain.risk.TradePlan;
import com.orbistrade.app.domain.strategy.StrategyDecision;
import com.orbistrade.app.domain.strategy.StrategySelector;
import com.orbistrade.app.domain.structure.MarketStructureAnalysis;
import com.orbistrade.app.domain.structure.MarketStructureAnalyzer;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final String SYMBOL = "EUR/USD";
    private static final int CANDLE_LIMIT = 240;

    private final MarketSnapshotFactory snapshotFactory = new MarketSnapshotFactory();
    private final BiasGuard biasGuard = new BiasGuard();
    private final EntrySetupPlanner entrySetupPlanner = new EntrySetupPlanner();
    private final MultiTimeframeAnalyzer multiTimeframeAnalyzer = new MultiTimeframeAnalyzer();
    private final SetupScorer setupScorer = new SetupScorer();
    private final MentorExplainer mentorExplainer = new MentorExplainer();
    private final MarketStructureAnalyzer structureAnalyzer = new MarketStructureAnalyzer();
    private final CandlestickPatternAnalyzer patternAnalyzer = new CandlestickPatternAnalyzer();
    private final ExecutorService marketExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EditText balanceInput = findViewById(R.id.balanceInput);
        EditText riskInput = findViewById(R.id.riskInput);
        RadioGroup thesisGroup = findViewById(R.id.thesisGroup);
        Button analyzeButton = findViewById(R.id.analyzeButton);
        Button apiSettingsButton = findViewById(R.id.apiSettingsButton);

        analyzeButton.setOnClickListener(view -> analyze(balanceInput, riskInput, thesisGroup, analyzeButton));
        apiSettingsButton.setOnClickListener(view -> startActivity(new Intent(this, ApiSettingsActivity.class)));
        analyze(balanceInput, riskInput, thesisGroup, analyzeButton);
    }

    @Override
    protected void onDestroy() {
        marketExecutor.shutdownNow();
        super.onDestroy();
    }

    private void analyze(EditText balanceInput, EditText riskInput, RadioGroup thesisGroup, Button analyzeButton) {
        final double balance;
        final double riskPercent;
        final TradeThesis thesis;
        try {
            balance = parseNumber(balanceInput.getText().toString());
            riskPercent = parseNumber(riskInput.getText().toString());
            thesis = readThesis(thesisGroup.getCheckedRadioButtonId());
        } catch (IllegalArgumentException exception) {
            showError(exception.getMessage());
            return;
        }

        analyzeButton.setEnabled(false);
        text(R.id.statusText).setText("Buscando candles, estrutura e padrões de preço...");

        marketExecutor.execute(() -> {
            try {
                MarketDataLoad data = loadMarketData();
                AnalysisOutput output = buildAnalysis(data, balance, riskPercent, thesis);
                runOnUiThread(() -> {
                    render(output);
                    analyzeButton.setEnabled(true);
                });
            } catch (RuntimeException exception) {
                runOnUiThread(() -> {
                    showError(exception.getMessage() == null ? "Falha inesperada ao processar os dados de mercado." : exception.getMessage());
                    analyzeButton.setEnabled(true);
                });
            }
        });
    }

    private MarketDataLoad loadMarketData() {
        ApiKeyStore keyStore = new ApiKeyStore(this);
        String selectedProvider = keyStore.readActiveMarketProvider();
        String apiKey = keyStore.read(ApiKeyStore.TWELVE_DATA_API_KEY);

        if ("Twelve Data".equalsIgnoreCase(selectedProvider) && !apiKey.isEmpty()) {
            try {
                MarketDataProvider provider = new TwelveDataMarketDataProvider(apiKey);
                return new MarketDataLoad(
                        provider.getCandles(SYMBOL, "15m", CANDLE_LIMIT),
                        provider.getCandles(SYMBOL, "5m", CANDLE_LIMIT),
                        "Twelve Data — mercado real",
                        ""
                );
            } catch (IllegalArgumentException exception) {
                MarketDataProvider fallback = new DemoMarketDataProvider();
                return new MarketDataLoad(
                        fallback.getCandles(SYMBOL, "15m", CANDLE_LIMIT),
                        fallback.getCandles(SYMBOL, "5m", CANDLE_LIMIT),
                        "Modo demonstração",
                        "A consulta real falhou: " + exception.getMessage()
                );
            }
        }

        MarketDataProvider fallback = new DemoMarketDataProvider();
        String reason = apiKey.isEmpty()
                ? "Configure a chave da Twelve Data para ativar candles reais."
                : "O provedor selecionado ainda não possui integração de candles.";
        return new MarketDataLoad(
                fallback.getCandles(SYMBOL, "15m", CANDLE_LIMIT),
                fallback.getCandles(SYMBOL, "5m", CANDLE_LIMIT),
                "Modo demonstração",
                reason
        );
    }

    private AnalysisOutput buildAnalysis(MarketDataLoad data, double balance, double riskPercent, TradeThesis thesis) {
        MarketSnapshot context15m = snapshotFactory.create(SYMBOL, data.contextCandles);
        MarketSnapshot trigger5m = snapshotFactory.create(SYMBOL, data.triggerCandles);
        StrategySelector.SelectionResult contextResult = new StrategySelector().analyze(context15m);
        StrategySelector.SelectionResult triggerResult = new StrategySelector().analyze(trigger5m);
        MarketStructureAnalysis contextStructure = structureAnalyzer.analyze(data.contextCandles);
        MarketStructureAnalysis triggerStructure = structureAnalyzer.analyze(data.triggerCandles);
        CandlestickPatternAnalysis contextPatterns = patternAnalyzer.analyze(data.contextCandles);
        CandlestickPatternAnalysis triggerPatterns = patternAnalyzer.analyze(data.triggerCandles);

        MultiTimeframeAnalysis multiTimeframe = multiTimeframeAnalyzer.analyze(
                context15m, contextResult.getRegime(), contextResult.getDecision(),
                trigger5m, triggerResult.getRegime(), triggerResult.getDecision()
        );

        StrategyDecision effectiveDecision = triggerResult.getDecision();
        if (multiTimeframe.getStatus() != MultiTimeframeAnalysis.Status.ALIGNED) {
            effectiveDecision = new StrategyDecision(
                    "Filtro multitemporal",
                    StrategyDecision.Action.WAIT,
                    multiTimeframe.getQualityScore(),
                    multiTimeframe.getInstruction()
            );
        }
        if (isStructureConflict(effectiveDecision, contextStructure, triggerStructure)) {
            effectiveDecision = waitDecision(effectiveDecision, "Filtro de estrutura",
                    "A direção proposta conflita com a estrutura de preço. Aguarde BOS, CHoCH ou novo pivô confirmado.");
        }
        if (isPatternConflict(effectiveDecision, triggerPatterns)) {
            effectiveDecision = waitDecision(effectiveDecision, "Filtro de padrões",
                    "O padrão recente aponta contra a direção sugerida. Aguarde confirmação no próximo candle.");
        }

        RiskProfile profile = new RiskProfile(balance, riskPercent, 2.0, 1.5);
        TradePlan plan = new RiskManager().buildPlan(trigger5m, effectiveDecision, profile);
        EntrySetup setup = entrySetupPlanner.build(trigger5m, effectiveDecision, plan);
        SetupAssessment assessment = setupScorer.score(context15m, trigger5m, multiTimeframe, effectiveDecision, plan);
        String mentor = mentorExplainer.explain(context15m, trigger5m, multiTimeframe, effectiveDecision, plan, assessment)
                + buildStructureMentorNote(contextStructure, triggerStructure)
                + buildPatternMentorNote(contextPatterns, triggerPatterns);
        ConfirmationReview review = biasGuard.review(
                thesis, trigger5m, triggerResult.getRegime(), effectiveDecision, plan
        );

        return new AnalysisOutput(
                data.sourceLabel, data.warning, context15m, trigger5m,
                contextResult, triggerResult, effectiveDecision, multiTimeframe,
                contextStructure, triggerStructure, contextPatterns, triggerPatterns,
                assessment, mentor, plan, setup, thesis, review
        );
    }

    private StrategyDecision waitDecision(StrategyDecision current, String name, String rationale) {
        return new StrategyDecision(name, StrategyDecision.Action.WAIT,
                Math.min(current.getConfidence(), 55), rationale);
    }

    private boolean isStructureConflict(StrategyDecision decision, MarketStructureAnalysis context, MarketStructureAnalysis trigger) {
        if (decision.getAction() == StrategyDecision.Action.BUY) {
            return context.getTrend() == MarketStructureAnalysis.Trend.BEARISH
                    || trigger.getEvent() == MarketStructureAnalysis.Event.CHOCH_BEARISH
                    || trigger.getEvent() == MarketStructureAnalysis.Event.BOS_BEARISH;
        }
        if (decision.getAction() == StrategyDecision.Action.SELL) {
            return context.getTrend() == MarketStructureAnalysis.Trend.BULLISH
                    || trigger.getEvent() == MarketStructureAnalysis.Event.CHOCH_BULLISH
                    || trigger.getEvent() == MarketStructureAnalysis.Event.BOS_BULLISH;
        }
        return false;
    }

    private boolean isPatternConflict(StrategyDecision decision, CandlestickPatternAnalysis patterns) {
        if (patterns.getConfidence() < 60) return false;
        return (decision.getAction() == StrategyDecision.Action.BUY
                && patterns.getBias() == CandlestickPatternAnalysis.Bias.BEARISH)
                || (decision.getAction() == StrategyDecision.Action.SELL
                && patterns.getBias() == CandlestickPatternAnalysis.Bias.BULLISH);
    }

    private String buildStructureMentorNote(MarketStructureAnalysis context, MarketStructureAnalysis trigger) {
        return "\n\nLeitura estrutural: no 15m a estrutura está " + translateTrend(context.getTrend())
                + "; no 5m está " + translateTrend(trigger.getTrend())
                + ". Evento recente: " + translateEvent(trigger.getEvent()) + ".";
    }

    private String buildPatternMentorNote(CandlestickPatternAnalysis context, CandlestickPatternAnalysis trigger) {
        return "\n\nPadrões de candles: 15m " + translatePatternBias(context.getBias())
                + "; 5m " + translatePatternBias(trigger.getBias())
                + ". Padrões isolados não autorizam entrada; use-os como confirmação da estrutura e do risco.";
    }

    private void render(AnalysisOutput output) {
        String status = "Fonte: " + output.sourceLabel + "\nAssistente de análise, risco e aprendizado ativo";
        if (!output.sourceWarning.isEmpty()) status += "\nAviso: " + output.sourceWarning;
        text(R.id.statusText).setText(status);
        text(R.id.regimeText).setText(formatMarket(output));
        text(R.id.strategyText).setText("Estratégia efetiva: " + output.effectiveDecision.getStrategyName());
        text(R.id.decisionText).setText("Decisão: " + output.effectiveDecision.getAction().name()
                + "\nConfiança: " + output.effectiveDecision.getConfidence() + "%"
                + "\nMotivo: " + output.effectiveDecision.getRationale());
        text(R.id.multiTimeframeText).setText(formatMultiTimeframe(output.multiTimeframe));
        text(R.id.marketStructureText).setText(formatStructure(output.contextStructure, output.triggerStructure));
        text(R.id.candlestickPatternText).setText(formatPatterns(output.contextPatterns, output.triggerPatterns));
        text(R.id.setupScoreText).setText(formatAssessment(output.assessment));
        text(R.id.mentorText).setText("Modo mentor\n" + output.mentorExplanation);
        text(R.id.riskPlanText).setText(formatPlan(output.plan));
        text(R.id.entrySetupText).setText(formatSetup(output.setup));
        text(R.id.biasReviewText).setText(formatReview(output.thesis, output.review));
    }

    private void showError(String message) {
        text(R.id.statusText).setText("Não foi possível concluir a análise");
        text(R.id.multiTimeframeText).setText("Análise multitemporal indisponível.");
        text(R.id.marketStructureText).setText("Estrutura de mercado indisponível.");
        text(R.id.candlestickPatternText).setText("Padrões de candles indisponíveis.");
        text(R.id.setupScoreText).setText("Qualidade do setup indisponível.");
        text(R.id.mentorText).setText("Modo mentor indisponível.");
        text(R.id.riskPlanText).setText(message == null ? "Erro desconhecido." : message);
        text(R.id.entrySetupText).setText("Setup intraday indisponível.");
        text(R.id.biasReviewText).setText("Revisão contra viés indisponível.");
    }

    private TextView text(int id) { return findViewById(id); }

    private String formatMarket(AnalysisOutput output) {
        return String.format(Locale.US,
                "15m: %s | preço %.5f | RSI %.1f\n5m: %s | preço %.5f | RSI %.1f | ATR %.2f%%",
                output.contextResult.getRegime().name(), output.context15m.getPrice(), output.context15m.getRsi(),
                output.triggerResult.getRegime().name(), output.trigger5m.getPrice(), output.trigger5m.getRsi(),
                output.trigger5m.getAtrPercent());
    }

    private String formatMultiTimeframe(MultiTimeframeAnalysis analysis) {
        return "Filtro multitemporal\nStatus: " + analysis.getStatus().name()
                + "\nQualidade: " + analysis.getQualityScore() + "%"
                + "\n" + analysis.getContextSummary() + "\n" + analysis.getTriggerSummary()
                + "\n\nOrientação: " + analysis.getInstruction();
    }

    private String formatStructure(MarketStructureAnalysis context, MarketStructureAnalysis trigger) {
        return "Estrutura de mercado\n15m: " + translateTrend(context.getTrend())
                + " | topo " + context.getLastHighLabel() + " | fundo " + context.getLastLowLabel()
                + " | confiança " + context.getConfidence() + "%"
                + "\n5m: " + translateTrend(trigger.getTrend())
                + " | topo " + trigger.getLastHighLabel() + " | fundo " + trigger.getLastLowLabel()
                + " | evento " + translateEvent(trigger.getEvent())
                + String.format(Locale.US, "\nSuporte: %.5f | Resistência: %.5f", trigger.getSupport(), trigger.getResistance())
                + "\nLiquidez: " + formatLiquidity(trigger)
                + "\n\nEvidências 5m:\n" + formatEvidence(trigger.getEvidence());
    }

    private String formatPatterns(CandlestickPatternAnalysis context, CandlestickPatternAnalysis trigger) {
        return "Padrões de candles"
                + "\n15m: " + translatePatternBias(context.getBias()) + " | confiança " + context.getConfidence() + "%"
                + "\nDetectados: " + formatPatternNames(context.getPatterns())
                + "\n\n5m: " + translatePatternBias(trigger.getBias()) + " | confiança " + trigger.getConfidence() + "%"
                + "\nDetectados: " + formatPatternNames(trigger.getPatterns())
                + "\n\nLeitura 5m:\n" + formatEvidence(trigger.getEvidence());
    }

    private String formatPatternNames(List<String> patterns) {
        if (patterns == null || patterns.isEmpty()) return "nenhum padrão forte";
        StringBuilder builder = new StringBuilder();
        for (String pattern : patterns) {
            if (builder.length() > 0) builder.append(", ");
            builder.append(pattern);
        }
        return builder.toString();
    }

    private String formatLiquidity(MarketStructureAnalysis analysis) {
        if (analysis.hasEqualHighs() && analysis.hasEqualLows()) return "acima dos topos e abaixo dos fundos equivalentes";
        if (analysis.hasEqualHighs()) return "provável concentração acima dos topos equivalentes";
        if (analysis.hasEqualLows()) return "provável concentração abaixo dos fundos equivalentes";
        return "nenhum agrupamento evidente nos pivôs recentes";
    }

    private String translateTrend(MarketStructureAnalysis.Trend trend) {
        switch (trend) {
            case BULLISH: return "altista (HH/HL)";
            case BEARISH: return "baixista (LH/LL)";
            case RANGE: return "lateral/mista";
            default: return "indefinida";
        }
    }

    private String translateEvent(MarketStructureAnalysis.Event event) {
        switch (event) {
            case BOS_BULLISH: return "BOS altista";
            case BOS_BEARISH: return "BOS baixista";
            case CHOCH_BULLISH: return "CHoCH altista";
            case CHOCH_BEARISH: return "CHoCH baixista";
            default: return "nenhum rompimento confirmado";
        }
    }

    private String translatePatternBias(CandlestickPatternAnalysis.Bias bias) {
        switch (bias) {
            case BULLISH: return "viés altista";
            case BEARISH: return "viés baixista";
            default: return "neutro/indecisão";
        }
    }

    private String formatAssessment(SetupAssessment assessment) {
        return "Qualidade do setup: " + assessment.getGrade() + " — " + assessment.getTotalScore() + "/100"
                + "\nTendência: " + assessment.getTrendScore() + "/20"
                + "\nTimeframes: " + assessment.getTimeframeScore() + "/20"
                + "\nMomentum: " + assessment.getMomentumScore() + "/20"
                + "\nVolatilidade: " + assessment.getVolatilityScore() + "/20"
                + "\nRisco: " + assessment.getRiskScore() + "/20"
                + "\n\nPontos fortes:\n" + formatEvidence(assessment.getStrengths())
                + "\n\nPontos fracos:\n" + formatEvidence(assessment.getWeaknesses());
    }

    private TradeThesis readThesis(int checkedId) {
        if (checkedId == R.id.thesisBuy) return TradeThesis.BUY;
        if (checkedId == R.id.thesisSell) return TradeThesis.SELL;
        return TradeThesis.NEUTRAL;
    }

    private double parseNumber(String value) {
        String normalized = value.trim().replace(',', '.');
        if (normalized.isEmpty()) throw new IllegalArgumentException("Preencha saldo e risco por operação.");
        try { return Double.parseDouble(normalized); }
        catch (NumberFormatException exception) { throw new IllegalArgumentException("Use apenas números válidos."); }
    }

    private String formatPlan(TradePlan plan) {
        if (!plan.isExecutable()) return "Plano de risco: BLOQUEADO\n" + plan.getNote();
        return String.format(Locale.US,
                "Plano de risco: LIBERADO\nEntrada: %.5f\nStop loss: %.5f\nTake profit: %.5f\nRisco financeiro: %.2f\nTamanho estimado: %.2f unidades\n%s",
                plan.getEntryPrice(), plan.getStopLoss(), plan.getTakeProfit(),
                plan.getRiskAmount(), plan.getPositionUnits(), plan.getNote());
    }

    private String formatSetup(EntrySetup setup) {
        if (setup.getStatus() == EntrySetup.Status.BLOCKED) {
            return "Setup de entrada: BLOQUEADO\n" + setup.getTriggerCondition() + "\n" + setup.getWarning();
        }
        return String.format(Locale.US,
                "Setup condicional de 15–30 min\nStatus: %s\nEstratégia: %s\nJanela estimada: até %d min\nZona de gatilho: %.5f\nInvalidação: %.5f\nConfirmação: %s\n\n%s",
                setup.getStatus().name(), setup.getStrategyName(), setup.getWindowMinutes(),
                setup.getTriggerPrice(), setup.getInvalidationPrice(), setup.getTriggerCondition(), setup.getWarning());
    }

    private String formatReview(TradeThesis thesis, ConfirmationReview review) {
        return "Revisão contra viés de confirmação\nSua hipótese: " + thesis.name()
                + "\nVeredito: " + review.getVerdict().name()
                + "\nAlinhamento: " + review.getAlignmentScore() + "%"
                + "\n\nA favor:\n" + formatEvidence(review.getSupportingEvidence())
                + "\n\nContra:\n" + formatEvidence(review.getOpposingEvidence())
                + "\n\nConclusão: " + review.getConclusion();
    }

    private String formatEvidence(List<String> evidence) {
        if (evidence == null || evidence.isEmpty()) return "• Nenhuma evidência relevante.";
        StringBuilder builder = new StringBuilder();
        for (String item : evidence) {
            if (builder.length() > 0) builder.append('\n');
            builder.append("• ").append(item);
        }
        return builder.toString();
    }

    private static final class MarketDataLoad {
        private final List<Candle> contextCandles;
        private final List<Candle> triggerCandles;
        private final String sourceLabel;
        private final String warning;

        private MarketDataLoad(List<Candle> contextCandles, List<Candle> triggerCandles, String sourceLabel, String warning) {
            this.contextCandles = contextCandles;
            this.triggerCandles = triggerCandles;
            this.sourceLabel = sourceLabel;
            this.warning = warning;
        }
    }

    private static final class AnalysisOutput {
        private final String sourceLabel;
        private final String sourceWarning;
        private final MarketSnapshot context15m;
        private final MarketSnapshot trigger5m;
        private final StrategySelector.SelectionResult contextResult;
        private final StrategySelector.SelectionResult triggerResult;
        private final StrategyDecision effectiveDecision;
        private final MultiTimeframeAnalysis multiTimeframe;
        private final MarketStructureAnalysis contextStructure;
        private final MarketStructureAnalysis triggerStructure;
        private final CandlestickPatternAnalysis contextPatterns;
        private final CandlestickPatternAnalysis triggerPatterns;
        private final SetupAssessment assessment;
        private final String mentorExplanation;
        private final TradePlan plan;
        private final EntrySetup setup;
        private final TradeThesis thesis;
        private final ConfirmationReview review;

        private AnalysisOutput(
                String sourceLabel, String sourceWarning,
                MarketSnapshot context15m, MarketSnapshot trigger5m,
                StrategySelector.SelectionResult contextResult,
                StrategySelector.SelectionResult triggerResult,
                StrategyDecision effectiveDecision,
                MultiTimeframeAnalysis multiTimeframe,
                MarketStructureAnalysis contextStructure,
                MarketStructureAnalysis triggerStructure,
                CandlestickPatternAnalysis contextPatterns,
                CandlestickPatternAnalysis triggerPatterns,
                SetupAssessment assessment, String mentorExplanation,
                TradePlan plan, EntrySetup setup, TradeThesis thesis, ConfirmationReview review
        ) {
            this.sourceLabel = sourceLabel;
            this.sourceWarning = sourceWarning;
            this.context15m = context15m;
            this.trigger5m = trigger5m;
            this.contextResult = contextResult;
            this.triggerResult = triggerResult;
            this.effectiveDecision = effectiveDecision;
            this.multiTimeframe = multiTimeframe;
            this.contextStructure = contextStructure;
            this.triggerStructure = triggerStructure;
            this.contextPatterns = contextPatterns;
            this.triggerPatterns = triggerPatterns;
            this.assessment = assessment;
            this.mentorExplanation = mentorExplanation;
            this.plan = plan;
            this.setup = setup;
            this.thesis = thesis;
            this.review = review;
        }
    }
}
