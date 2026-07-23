package com.orbistrade.app.data.market;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public final class TwelveDataMarketDataProvider implements MarketDataProvider {
    private static final String BASE_URL = "https://api.twelvedata.com/time_series";
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 15_000;

    private final String apiKey;

    public TwelveDataMarketDataProvider(String apiKey) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        if (this.apiKey.isEmpty()) {
            throw new IllegalArgumentException("Configure a chave da Twelve Data na Central de APIs.");
        }
    }

    @Override
    public List<Candle> getCandles(String symbol, String interval, int limit) {
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new IllegalArgumentException("Símbolo de mercado inválido.");
        }
        if (limit < 20 || limit > 5_000) {
            throw new IllegalArgumentException("A quantidade de candles deve ficar entre 20 e 5000.");
        }

        HttpURLConnection connection = null;
        try {
            String requestUrl = BASE_URL
                    + "?symbol=" + encode(symbol.trim())
                    + "&interval=" + encode(normalizeInterval(interval))
                    + "&outputsize=" + limit
                    + "&timezone=UTC"
                    + "&format=JSON"
                    + "&apikey=" + encode(apiKey);

            connection = (HttpURLConnection) new URL(requestUrl).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestProperty("Accept", "application/json");

            int statusCode = connection.getResponseCode();
            InputStream stream = statusCode >= 200 && statusCode < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();
            String payload = readBody(stream);

            if (payload.isEmpty()) {
                throw new IllegalArgumentException("A Twelve Data respondeu sem conteúdo.");
            }

            JSONObject response = new JSONObject(payload);
            if (statusCode < 200 || statusCode >= 300 || "error".equalsIgnoreCase(response.optString("status"))) {
                String message = response.optString("message", "Falha ao consultar a Twelve Data.");
                throw new IllegalArgumentException("Twelve Data: " + message);
            }

            JSONArray values = response.optJSONArray("values");
            if (values == null || values.length() < 20) {
                throw new IllegalArgumentException("A Twelve Data não retornou candles suficientes para a análise.");
            }

            List<Candle> candles = new ArrayList<>(values.length());
            for (int index = 0; index < values.length(); index++) {
                JSONObject value = values.getJSONObject(index);
                candles.add(new Candle(
                        parseTimestamp(value.getString("datetime")),
                        parseDouble(value, "open"),
                        parseDouble(value, "high"),
                        parseDouble(value, "low"),
                        parseDouble(value, "close"),
                        parseOptionalDouble(value, "volume")
                ));
            }

            // A API entrega o candle mais recente primeiro; o motor usa ordem cronológica.
            Collections.reverse(candles);
            return candles;
        } catch (IOException exception) {
            throw new IllegalArgumentException("Não foi possível conectar à Twelve Data. Verifique a internet.", exception);
        } catch (JSONException | ParseException exception) {
            throw new IllegalArgumentException("A resposta da Twelve Data veio em formato inesperado.", exception);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String normalizeInterval(String interval) {
        if (interval == null) {
            throw new IllegalArgumentException("Intervalo inválido.");
        }
        switch (interval.trim().toLowerCase(Locale.US)) {
            case "1m": return "1min";
            case "5m": return "5min";
            case "15m": return "15min";
            case "30m": return "30min";
            case "1h": return "1h";
            case "4h": return "4h";
            case "1d": return "1day";
            default: throw new IllegalArgumentException("Intervalo não suportado pela integração: " + interval);
        }
    }

    private long parseTimestamp(String value) throws ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date date = formatter.parse(value);
        if (date == null) {
            throw new ParseException("Data inválida", 0);
        }
        return date.getTime();
    }

    private double parseDouble(JSONObject object, String field) throws JSONException {
        return Double.parseDouble(object.getString(field));
    }

    private double parseOptionalDouble(JSONObject object, String field) {
        String value = object.optString(field, "0");
        if (value.isEmpty() || "null".equalsIgnoreCase(value)) {
            return 0.0;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return 0.0;
        }
    }

    private String readBody(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
