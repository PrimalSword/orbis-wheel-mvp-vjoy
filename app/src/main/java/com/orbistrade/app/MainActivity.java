package com.orbistrade.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.orbistrade.app.data.market.DemoMarketDataProvider;
import com.orbistrade.app.data.market.MarketDataProvider;
import com.orbistrade.app.domain.market.MarketSnapshotFactory;
import com.orbistrade.app.domain.model.MarketSnapshot;
import com.orbistrade.app.domain.risk.RiskManager;
import com.orbistrade.app.domain.risk.RiskProfile;
import com.orbistrade.app.domain.risk.TradePlan;
import com.orbistrade.app.domain.strategy.StrategyDecision;
import com.orbistrade.app.domain.strategy.StrategySelector;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private final MarketDataProvider marketDataProvider = new DemoMarketDataProvider();
    private final MarketSnapshotFactory snapshotFactory = new MarketSnapshotFactory();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EditText balanceInput = findViewById(R.id.balanceInput);
        EditText riskInput = findViewById(R.id.riskInput);
        Button analyzeButton = findViewById(R.id.analyzeButton);
        Button apiSettingsButton = findViewById(R.id.apiSettingsButton);

        analyzeButton.setOnClickListener(view -> analyze(balanceInput, riskInput));
        apiSettingsButton.setOnClickListener(view ->
                startActivity(new Intent(this, ApiSettingsActivity.class))
        );
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

            MarketSnapshot snapshot = snapshotFactory.create(
                    "EUR/USD",
                    marketDataProvider.getCandles("EUR/USD", "1h", 240)
            );

            StrategySelector.SelectionResult result = new StrategySelector().analyze(snapshot);
            StrategyDecision decision = result.getDecision();

            RiskProfile profile = new RiskProfile(balance, riskPercent, 2.0, 1.5);
            TradePlan plan = new RiskManager().buildPlan(snapshot, decision, profile);

            statusText.setText("Dados, indicadores, estratégia e risco ativos");
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
        } catch (IllegalArgumentException exception) {
            statusText.setText("Não foi possível concluir a análise");
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
