package com.orbistrade.app.data.market;

import java.util.ArrayList;
import java.util.List;

public final class DemoMarketDataProvider implements MarketDataProvider {

    @Override
    public List<Candle> getCandles(String symbol, String interval, int limit) {
        int count = Math.max(220, limit);
        List<Candle> candles = new ArrayList<>(count);
        double previousClose = 1.0750;
        long now = System.currentTimeMillis();
        long stepMillis = 60L * 60L * 1000L;

        for (int index = 0; index < count; index++) {
            double trend = index * 0.000065;
            double wave = Math.sin(index / 7.0) * 0.00115;
            double close = 1.0750 + trend + wave;
            double open = previousClose;
            double range = 0.00065 + Math.abs(Math.cos(index / 5.0)) * 0.00035;
            double high = Math.max(open, close) + range;
            double low = Math.min(open, close) - range;
            double volume = 900 + (index % 24) * 35;

            candles.add(new Candle(
                    now - ((long) (count - index) * stepMillis),
                    open,
                    high,
                    low,
                    close,
                    volume
            ));
            previousClose = close;
        }

        if (limit >= candles.size()) {
            return candles;
        }
        return new ArrayList<>(candles.subList(candles.size() - limit, candles.size()));
    }
}