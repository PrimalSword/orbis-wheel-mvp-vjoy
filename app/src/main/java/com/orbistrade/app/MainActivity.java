package com.orbistrade.app;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.orbistrade.app.domain.model.MarketSnapshot;
import com.orbistrade.app.domain.strategy.StrategyDecision;
import com.orbistrade.app.domain.strategy.StrategySelector;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView statusText = findViewById(R.id.statusText);
        TextView regimeText = findViewById(R.id.regimeText);
        TextView strategyText = findViewById(R.id.strategyText);
        TextView decisionText = findViewById(R.id.decisionText);

        MarketSnapshot demoSnapshot = new MarketSnapshot(
                "EUR/USD",
                1.0890,
                1.0875,
                1.0810,
                58.0,
                0.85
        );

        StrategySelector.SelectionResult result = new StrategySelector().analyze(demoSnapshot);
        StrategyDecision decision = result.getDecision();

        statusText.setText("Motor adaptativo inicial ativo");
        regimeText.setText("Regime: " + result.getRegime().name());
        strategyText.setText("Estratégia: " + decision.getStrategyName());
        decisionText.setText(
                "Decisão: " + decision.getAction().name()
                        + "\nConfiança: " + decision.getConfidence() + "%"
                        + "\nMotivo: " + decision.getRationale()
        );
    }
}
