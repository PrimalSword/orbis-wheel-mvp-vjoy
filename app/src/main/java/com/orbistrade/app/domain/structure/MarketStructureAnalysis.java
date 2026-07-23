package com.orbistrade.app.domain.structure;

import java.util.Collections;
import java.util.List;

public final class MarketStructureAnalysis {
    public enum Trend { BULLISH, BEARISH, RANGE, UNDEFINED }
    public enum Event { BOS_BULLISH, BOS_BEARISH, CHOCH_BULLISH, CHOCH_BEARISH, NONE }

    private final Trend trend;
    private final Event event;
    private final String lastHighLabel;
    private final String lastLowLabel;
    private final double resistance;
    private final double support;
    private final boolean equalHighs;
    private final boolean equalLows;
    private final int confidence;
    private final List<String> evidence;

    public MarketStructureAnalysis(
            Trend trend,
            Event event,
            String lastHighLabel,
            String lastLowLabel,
            double resistance,
            double support,
            boolean equalHighs,
            boolean equalLows,
            int confidence,
            List<String> evidence
    ) {
        this.trend = trend;
        this.event = event;
        this.lastHighLabel = lastHighLabel;
        this.lastLowLabel = lastLowLabel;
        this.resistance = resistance;
        this.support = support;
        this.equalHighs = equalHighs;
        this.equalLows = equalLows;
        this.confidence = Math.max(0, Math.min(100, confidence));
        this.evidence = Collections.unmodifiableList(evidence);
    }

    public Trend getTrend() { return trend; }
    public Event getEvent() { return event; }
    public String getLastHighLabel() { return lastHighLabel; }
    public String getLastLowLabel() { return lastLowLabel; }
    public double getResistance() { return resistance; }
    public double getSupport() { return support; }
    public boolean hasEqualHighs() { return equalHighs; }
    public boolean hasEqualLows() { return equalLows; }
    public int getConfidence() { return confidence; }
    public List<String> getEvidence() { return evidence; }
}
