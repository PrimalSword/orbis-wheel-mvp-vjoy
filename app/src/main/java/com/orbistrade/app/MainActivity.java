package com.orbistrade.app;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.orbistrade.app.domain.model.MarketSnapshot;
import com.orbistrade.app.domain.risk.RiskManager;
import com.orbistrade.app.domain.risk.RiskProfile;
import com.orbistrade.app.domain.risk.TradePlan;
import com.orbistrade.app.domain.strategy.StrategyDecision;
import com.orbistrade.app.domain.strategy.StrategySelector;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private final MarketSnapshot demoSnapshot = new MarketSnapshot(
            "EUR/USD",
            1.0890,
            1.0875,
            1.0810,
            58.0,
            0.85
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EditText balanceInput = findViewById(R.id.balanceInput);
        EditText riskInput = findViewById(R.id.riskInput);
        Button analyzeButton = findViewById(R.id.analyzeButton);

        analyzeButton.setOnClickListener(view -> analyze(balanceInput, riskInput));
        analyze(balanceInput, riskInput);
    }

    private void analyze(EditText balanceInput, EditText riskInput) {
        TextView statusText = findViewById(R.id.statusText);
        TextView regimeText = findViewById(R.id.regimeText);
        TextView strategyText = findViewById(R.id.strategyText);
        TextView decisionText = findViewById(R.id.decisionText);
        TextView riskPlanText = findViewById(R.id.riskPlanText);

        try {
            double balance = parseNumber(balanceInput.getText().toString());
            double riskPercent = parseNumber(riskInput.getText().toString());

            StrategySelector.SelectionResult result = new StrategySelector().analyze(demoSnapshot);
            StrategyDecision decision = result.getDecision();

            RiskProfile profile = new RiskProfile(balance, riskPercent, 2.0, 1.5);
            TradePlan plan = new RiskManager().buildPlan(demoSnapshot, decision, profile);

            statusText.setText("Motor adaptativo e gestão de risco ativos");
            regimeText.setText("Regime: " + result.getRegime().name());
            strategyText.setText("Estratégia: " + decision.getStrategyName());
            decisionText.setText(
                    "Decisão: " + decision.getAction().name()
                            + "\nConfiança: " + decision.getConfidence() + "%"
                            + "\nMotivo: " + decision.getRationale()
            );

            riskPlanText.setText(formatPlan(plan));
        } catch (IllegalArgumentException exception) {
            statusText.setText("Revise os dados de risco");
            riskPlanText.setText(exception.getMessage());
        }
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
}