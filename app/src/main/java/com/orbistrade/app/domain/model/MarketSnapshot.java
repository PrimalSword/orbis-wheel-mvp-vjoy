package com.orbistrade.app.domain.model;

public final class MarketSnapshot {
    private final String symbol;
    private final double price;
    private final double ema20;
    private final double ema200;
    private final double rsi;
    private final double atrPercent;

    public MarketSnapshot(
            String symbol,
            double price,
            double ema20,
            double ema200,
            double rsi,
            double atrPercent
    ) {
        this.symbol = symbol;
        this.price = price;
        this.ema20 = ema20;
        this.ema200 = ema200;
        this.rsi = rsi;
        this.atrPercent = atrPercent;
    }

    public String getSymbol() {
        return symbol;
    }

    public double getPrice() {
        return price;
    }

    public double getEma20() {
        return ema20;
    }

    public double getEma200() {
        return ema200;
    }

    public double getRsi() {
        return rsi;
    }

    public double getAtrPercent() {
        return atrPercent;
    }
}
