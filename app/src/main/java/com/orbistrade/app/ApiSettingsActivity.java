package com.orbistrade.app;

import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.orbistrade.app.data.settings.ApiKeyStore;

public class ApiSettingsActivity extends AppCompatActivity {
    private ApiKeyStore apiKeyStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_api_settings);

        apiKeyStore = new ApiKeyStore(this);

        EditText geminiInput = findViewById(R.id.geminiApiInput);
        EditText marketInput = findViewById(R.id.marketApiInput);
        EditText brokerInput = findViewById(R.id.brokerApiInput);
        TextView statusText = findViewById(R.id.apiStatusText);
        Button saveButton = findViewById(R.id.saveApiButton);
        Button toggleButton = findViewById(R.id.toggleApiVisibilityButton);

        geminiInput.setText(apiKeyStore.read(ApiKeyStore.GEMINI_API_KEY));
        marketInput.setText(apiKeyStore.read(ApiKeyStore.MARKET_API_KEY));
        brokerInput.setText(apiKeyStore.read(ApiKeyStore.BROKER_API_KEY));
        updateStatus(statusText);

        saveButton.setOnClickListener(view -> {
            try {
                apiKeyStore.save(ApiKeyStore.GEMINI_API_KEY, geminiInput.getText().toString());
                apiKeyStore.save(ApiKeyStore.MARKET_API_KEY, marketInput.getText().toString());
                apiKeyStore.save(ApiKeyStore.BROKER_API_KEY, brokerInput.getText().toString());
                updateStatus(statusText);
            } catch (IllegalStateException exception) {
                statusText.setText(exception.getMessage());
            }
        });

        toggleButton.setOnClickListener(view -> {
            boolean hidden = geminiInput.getInputType() != InputType.TYPE_CLASS_TEXT;
            int type = hidden
                    ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD;
            geminiInput.setInputType(type);
            marketInput.setInputType(type);
            brokerInput.setInputType(type);
            geminiInput.setSelection(geminiInput.length());
            marketInput.setSelection(marketInput.length());
            brokerInput.setSelection(brokerInput.length());
            toggleButton.setText(hidden ? "Ocultar chaves" : "Mostrar chaves");
        });
    }

    private void updateStatus(TextView statusText) {
        int configured = 0;
        if (apiKeyStore.has(ApiKeyStore.GEMINI_API_KEY)) configured++;
        if (apiKeyStore.has(ApiKeyStore.MARKET_API_KEY)) configured++;
        if (apiKeyStore.has(ApiKeyStore.BROKER_API_KEY)) configured++;
        statusText.setText(configured + " de 3 integrações configuradas. As chaves ficam criptografadas neste aparelho.");
    }
}
