package com.orbistrade.app.data.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public final class ApiKeyStore {
    public static final String TWELVE_DATA_API_KEY = "twelve_data_api_key";
    public static final String OPENAI_API_KEY = "openai_api_key";
    public static final String GEMINI_API_KEY = "gemini_api_key";
    public static final String ANTHROPIC_API_KEY = "anthropic_api_key";
    public static final String BINANCE_API_KEY = "binance_api_key";
    public static final String BINANCE_API_SECRET = "binance_api_secret";
    public static final String OANDA_API_TOKEN = "oanda_api_token";
    public static final String OANDA_ACCOUNT_ID = "oanda_account_id";
    public static final String ALPHA_VANTAGE_API_KEY = "alpha_vantage_api_key";
    public static final String POLYGON_API_KEY = "polygon_api_key";
    public static final String ACTIVE_MARKET_PROVIDER = "active_market_provider";

    // Mantidos para compatibilidade com versões anteriores do app.
    public static final String MARKET_API_KEY = "market_api_key";
    public static final String BROKER_API_KEY = "broker_api_key";

    private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";
    private static final String KEY_ALIAS = "orbistrade_api_master_key";
    private static final String PREFS_NAME = "orbistrade_secure_api_settings";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    private final SharedPreferences preferences;

    public ApiKeyStore(Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        migrateLegacyKeys();
    }

    public void save(String name, String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            remove(name);
            return;
        }

        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey());
            byte[] encrypted = cipher.doFinal(normalized.getBytes(StandardCharsets.UTF_8));

            String iv = Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP);
            String payload = Base64.encodeToString(encrypted, Base64.NO_WRAP);
            preferences.edit().putString(name, iv + ":" + payload).apply();
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível proteger a chave de API.", exception);
        }
    }

    public String read(String name) {
        String stored = preferences.getString(name, "");
        if (stored == null || stored.isEmpty()) {
            return "";
        }

        try {
            String[] parts = stored.split(":", 2);
            if (parts.length != 2) {
                return "";
            }

            byte[] iv = Base64.decode(parts[0], Base64.NO_WRAP);
            byte[] payload = Base64.decode(parts[1], Base64.NO_WRAP);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), new GCMParameterSpec(128, iv));
            byte[] decrypted = cipher.doFinal(payload);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception exception) {
            return "";
        }
    }

    public boolean has(String name) {
        return !read(name).isEmpty();
    }

    public void remove(String name) {
        preferences.edit().remove(name).apply();
    }

    public void saveActiveMarketProvider(String provider) {
        preferences.edit().putString(ACTIVE_MARKET_PROVIDER, provider).apply();
    }

    public String readActiveMarketProvider() {
        return preferences.getString(ACTIVE_MARKET_PROVIDER, "Twelve Data");
    }

    private void migrateLegacyKeys() {
        if (!has(TWELVE_DATA_API_KEY) && has(MARKET_API_KEY)) {
            save(TWELVE_DATA_API_KEY, read(MARKET_API_KEY));
        }
    }

    private SecretKey getOrCreateSecretKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
        keyStore.load(null);

        if (keyStore.containsAlias(KEY_ALIAS)) {
            return ((KeyStore.SecretKeyEntry) keyStore.getEntry(KEY_ALIAS, null)).getSecretKey();
        }

        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER);
        generator.init(new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
        )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build());
        return generator.generateKey();
    }
}
