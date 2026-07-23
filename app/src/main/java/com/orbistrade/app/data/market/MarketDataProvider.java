package com.orbistrade.app.data.market;

import java.util.List;

public interface MarketDataProvider {
    List<Candle> getCandles(String symbol, String interval, int limit);
}