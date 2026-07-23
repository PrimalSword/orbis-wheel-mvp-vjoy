package com.orbistrade.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.orbistrade.app.data.market.DemoMarketDataProvider;
import com.orbistrade.app.data.market.MarketDataProvider;
import com.orbistrade.app.domain.assistant.BiasGuard;
import com.orbistrade.app.domain.assistant.ConfirmationReview;
import com.orbistrade.app.domain.assistant.EntrySetup;
import com.orbistrade.app.domain.assistant.EntrySetupPlanner;
import com.orbistrade.app.domain.assistant.TradeThesis;
import com.orbistrade.app.domain.market.MarketSnapshotFactory;
import com.orbistrade.app.domain.model.MarketSnapshot;
import com.orbistrade.app.domain.risk.RiskManager;
import com.orbistrade.app.domain.risk.RiskProfile;
import com.orbistrade.app.domain.risk.TradePlan;
import com.orbistrade.app.domain.strategy.StrategyDecision;
import com.orbistrade.app.domain.strategy.StrategySelector;

import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private final MarketDataProvider marketDataProvider = new DemoMarketDataProvider();
    private final MarketSnapshotFactory snapshotFactory = new MarketSnapshotFactory();
    private final BiasGuard biasGuard = new BiasGuard();
    private final EntrySetupPlanner entrySetupPlanner = new EntrySetupPlanner();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EditText balanceInput = findViewById(R.id.balanceInput);
        EditText riskInput = findViewById(R.id.riskInput);
        RadioGroup thesisGroup = findViewById(R.id.thesisGroup);
        Button analyzeButton = findViewById(R.id.analyzeButton);
        Button apiSettingsButton = findViewById(R.id.apiSettingsButton);

        analyzeButton.setOnClickListener(view -> analyze(balanceInput, riskInput, thesisGroup));
        apiSettingsButton.setOnClickListener(view ->
                startActivity(new Intent(this, ApiSettingsActivity.class))
        );
        analyze(balanceInput, riskInput, thesisGroup);
    }

    private void analyze(EditText balanceInput, EditText riskInput, RadioGroup thesisGroup) {
        TextView statusText = findViewById(R.id.statusText);
        TextView regimeText = findViewById(R.id.regimeText);
        TextView strategyText = findViewById(R.id.strategyText);
        TextView decisionText = findViewById(R.id.decisionText);
        TextView riskPlanText = findViewById(R.id.riskPlanText);
        TextView entrySetupText = findViewById(R.id.entrySetupText);
        TextView biasReviewText = findViewById(R.id.biasReviewText);

        try {
            double balance = parseNumber(balanceInput.getText().toString());
            double riskPercent = parseNumber(riskInput.getText().toString());
            TradeThesis thesis = readThesis(thesisGroup.getCheckedRadioButtonId());

            MarketSnapshot snapshot = snapshotFactory.create(
                    "EUR/USD",
                    marketDataProvider.getCandles("EUR/USD", "5m", 240)
            );

            StrategySelector.SelectionResult result = new StrategySelector().analyze(snapshot);
            StrategyDecision decision = result.getDecision();

            RiskProfile profile = new RiskProfile(balance, riskPercent, 2.0, 1.5);
            TradePlan plan = new RiskManager().buildPlan(snapshot, decision, profile);
            EntrySetup setup = entrySetupPlanner.build(snapshot, decision, plan);
            ConfirmationReview review = biasGuard.review(
                    thesis,
                    snapshot,
                    result.getRegime(),
                    decision,
                    plan
            );

            statusText.setText("Assistente intraday de 15 a 30 minutos ativo");
            regimeText.setText(
                    String.format(
                            Locale.US,
                            "Regime: %s\nPreço: %.5f | EMA20: %.5f | EMA200: %.5f\nRSI: %.1f | ATR: %.2f%%",
                            result.getRegime().name(),
                            snapshot.getPrice(),
                            snapshot.getEma20(),
                            snapshot.getEma200(),
                            snapshot.getRsi(),
                            snapshot.getAtrPercent()
                    )
            );
            strategyText.setText("Estratégia: " + decision.getStrategyName());
            decisionText.setText(
                    "Decisão: " + decision.getAction().name()
                            + "\nConfiança: " + decision.getConfidence() + "%"
                            + "\nMotivo: " + decision.getRationale()
            );

            riskPlanText.setText(formatPlan(plan));
            entrySetupText.setText(formatSetup(setup));
            biasReviewText.setText(formatReview(thesis, review));
        } catch (IllegalArgumentException exception) {
            statusText.setText("Não foi possível concluir a análise");
            riskPlanText.setText(exception.getMessage());
            entrySetupText.setText("Setup intraday indisponível.");
            biasReviewText.setText("Revisão contra viés indisponível.");
        }
    }

    private TradeThesis readThesis(int checkedId) {
        if (checkedId == R.id.thesisBuy) {
            return TradeThesis.BUY;
        }
        if (checkedId == R.id.thesisSell) {
            return TradeThesis.SELL;
        }
        return TradeThesis.NEUTRAL;
    }

    private double parseNumber(String value) {
        String normalized = value.trim().replace(',', '.');
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Preencha saldo e risco por operação.");
        }

        try {
            return Double.parseDouble(normalized);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Use apenas números válidos.");
        }
    }

    private String formatPlan(TradePlan plan) {
        if (!plan.isExecutable()) {
            return "Plano de risco: BLOQUEADO\n" + plan.getNote();
        }

        return String.format(
                Locale.US,
                "Plano de risco: LIBERADO\nEntrada: %.5f\nStop loss: %.5f\nTake profit: %.5f\nRisco financeiro: %.2f\nTamanho estimado: %.2f unidades\n%s",
                plan.getEntryPrice(),
                plan.getStopLoss(),
                plan.getTakeProfit(),
                plan.getRiskAmount(),
                plan.getPositionUnits(),
                plan.getNote()
        );
    }

    private String formatSetup(EntrySetup setup) {
        if (setup.getStatus() == EntrySetup.Status.BLOCKED) {
            return "Setup de entrada: BLOQUEADO\n" + setup.getTriggerCondition()
                    + "\n" + setup.getWarning();
        }

        return String.format(
                Locale.US,
                "Setup condicional de 15–30 min\nStatus: %s\nEstratégia: %s\nJanela estimada: até %d min\nZona de gatilho: %.5f\nInvalidação: %.5f\nConfirmação: %s\n\n%s",
                setup.getStatus().name(),
                setup.getStrategyName(),
                setup.getWindowMinutes(),
                setup.getTriggerPrice(),
                setup.getInvalidationPrice(),
                setup.getTriggerCondition(),
                setup.getWarning()
        );
    }

    private String formatReview(TradeThesis thesis, ConfirmationReview review) {
        return "Revisão contra viés de confirmação"
                + "\nSua hipótese: " + thesis.name()
                + "\nVeredito: " + review.getVerdict().name()
                + "\nAlinhamento: " + review.getAlignmentScore() + "%"
                + "\n\nA favor:\n" + formatEvidence(review.getSupportingEvidence())
                + "\n\nContra:\n" + formatEvidence(review.getOpposingEvidence())
                + "\n\nConclusão: " + review.getConclusion();
    }

    private String formatEvidence(List<String> evidence) {
        if (evidence.isEmpty()) {
            return "• Nenhuma evidência relevante.";
        }

        StringBuilder builder = new StringBuilder();
        for (String item : evidence) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append("• ").append(item);
        }
        return builder.toString();
    }
}