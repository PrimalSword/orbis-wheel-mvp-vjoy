package com.orbistrade.app;

import android.os.Bundle;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.orbistrade.app.data.settings.ApiKeyStore;

import java.util.LinkedHashMap;
import java.util.Map;

public class ApiSettingsActivity extends AppCompatActivity {
    private ApiKeyStore apiKeyStore;
    private boolean keysVisible;
    private final Map<String, EditText> fields = new LinkedHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_api_settings);

        apiKeyStore = new ApiKeyStore(this);

        TextView statusText = findViewById(R.id.apiStatusText);
        Spinner providerSpinner = findViewById(R.id.marketProviderSpinner);
        Button saveButton = findViewById(R.id.saveApiButton);
        Button removeButton = findViewById(R.id.removeApiButton);
        Button toggleButton = findViewById(R.id.toggleApiVisibilityButton);

        bindField(ApiKeyStore.TWELVE_DATA_API_KEY, R.id.twelveDataApiInput);
        bindField(ApiKeyStore.OPENAI_API_KEY, R.id.openAiApiInput);
        bindField(ApiKeyStore.GEMINI_API_KEY, R.id.geminiApiInput);
        bindField(ApiKeyStore.ANTHROPIC_API_KEY, R.id.anthropicApiInput);
        bindField(ApiKeyStore.BINANCE_API_KEY, R.id.binanceApiInput);
        bindField(ApiKeyStore.BINANCE_API_SECRET, R.id.binanceSecretInput);
        bindField(ApiKeyStore.OANDA_API_TOKEN, R.id.oandaTokenInput);
        bindField(ApiKeyStore.OANDA_ACCOUNT_ID, R.id.oandaAccountInput);
        bindField(ApiKeyStore.ALPHA_VANTAGE_API_KEY, R.id.alphaVantageApiInput);
        bindField(ApiKeyStore.POLYGON_API_KEY, R.id.polygonApiInput);

        String[] providers = {"Twelve Data", "OANDA", "Binance", "Alpha Vantage", "Polygon.io", "Demo"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                providers
        );
        providerSpinner.setAdapter(adapter);
        selectProvider(providerSpinner, providers, apiKeyStore.readActiveMarketProvider());

        loadFields();
        updateStatus(statusText);

        saveButton.setOnClickListener(view -> {
            try {
                for (Map.Entry<String, EditText> entry : fields.entrySet()) {
                    apiKeyStore.save(entry.getKey(), entry.getValue().getText().toString());
                }
                apiKeyStore.saveActiveMarketProvider(providerSpinner.getSelectedItem().toString());
                updateStatus(statusText);
                statusText.append("\nConfigurações salvas com segurança.");
            } catch (IllegalStateException exception) {
                statusText.setText(exception.getMessage());
            }
        });

        removeButton.setOnClickListener(view -> {
            for (String key : fields.keySet()) {
                apiKeyStore.remove(key);
            }
            loadFields();
            updateStatus(statusText);
            statusText.append("\nTodas as chaves foram removidas deste aparelho.");
        });

        toggleButton.setOnClickListener(view -> {
            keysVisible = !keysVisible;
            int type = InputType.TYPE_CLASS_TEXT | (keysVisible
                    ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    : InputType.TYPE_TEXT_VARIATION_PASSWORD);
            for (EditText field : fields.values()) {
                field.setInputType(type);
                field.setSelection(field.length());
            }
            toggleButton.setText(keysVisible ? "Ocultar chaves" : "Mostrar chaves");
        });
    }

    private void bindField(String key, int viewId) {
        fields.put(key, findViewById(viewId));
    }

    private void loadFields() {
        for (Map.Entry<String, EditText> entry : fields.entrySet()) {
            entry.getValue().setText(apiKeyStore.read(entry.getKey()));
        }
    }

    private void selectProvider(Spinner spinner, String[] providers, String selected) {
        for (int index = 0; index < providers.length; index++) {
            if (providers[index].equals(selected)) {
                spinner.setSelection(index);
                return;
            }
        }
    }

    private void updateStatus(TextView statusText) {
        int configured = 0;
        for (String key : fields.keySet()) {
            if (apiKeyStore.has(key)) {
                configured++;
            }
        }

        String twelveStatus = apiKeyStore.has(ApiKeyStore.TWELVE_DATA_API_KEY)
                ? "✓ Twelve Data configurada"
                : "○ Twelve Data não configurada";
        String aiStatus = apiKeyStore.has(ApiKeyStore.OPENAI_API_KEY)
                || apiKeyStore.has(ApiKeyStore.GEMINI_API_KEY)
                || apiKeyStore.has(ApiKeyStore.ANTHROPIC_API_KEY)
                ? "✓ Pelo menos uma IA configurada"
                : "○ IA ainda não configurada";

        statusText.setText(
                configured + " credenciais configuradas"
                        + "\nFonte ativa: " + apiKeyStore.readActiveMarketProvider()
                        + "\n" + twelveStatus
                        + "\n" + aiStatus
                        + "\nAs chaves ficam criptografadas neste aparelho."
        );
    }
}
