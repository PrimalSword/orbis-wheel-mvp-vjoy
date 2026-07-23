package com.orbistrade.app.domain.pattern;

import java.util.Collections;
import java.util.List;

public final class CandlestickPatternAnalysis {
    public enum Bias { BULLISH, BEARISH, NEUTRAL }

    private final Bias bias;
    private final int confidence;
    private final List<String> patterns;
    private final List<String> evidence;

    public CandlestickPatternAnalysis(Bias bias, int confidence, List<String> patterns, List<String> evidence) {
        this.bias = bias;
        this.confidence = Math.max(0, Math.min(100, confidence));
        this.patterns = Collections.unmodifiableList(patterns);
        this.evidence = Collections.unmodifiableList(evidence);
    }

    public Bias getBias() { return bias; }
    public int getConfidence() { return confidence; }
    public List<String> getPatterns() { return patterns; }
    public List<String> getEvidence() { return evidence; }
    public boolean hasSignal() { return !patterns.isEmpty(); }
}
