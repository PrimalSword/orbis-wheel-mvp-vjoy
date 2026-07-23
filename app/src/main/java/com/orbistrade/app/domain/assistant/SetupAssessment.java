package com.orbistrade.app.domain.assistant;

import java.util.Collections;
import java.util.List;

public final class SetupAssessment {

    private final int totalScore;
    private final String grade;
    private final int trendScore;
    private final int timeframeScore;
    private final int momentumScore;
    private final int volatilityScore;
    private final int riskScore;
    private final List<String> strengths;
    private final List<String> weaknesses;

    public SetupAssessment(
            int totalScore,
            String grade,
            int trendScore,
            int timeframeScore,
            int momentumScore,
            int volatilityScore,
            int riskScore,
            List<String> strengths,
            List<String> weaknesses
    ) {
        this.totalScore = totalScore;
        this.grade = grade;
        this.trendScore = trendScore;
        this.timeframeScore = timeframeScore;
        this.momentumScore = momentumScore;
        this.volatilityScore = volatilityScore;
        this.riskScore = riskScore;
        this.strengths = Collections.unmodifiableList(strengths);
        this.weaknesses = Collections.unmodifiableList(weaknesses);
    }

    public int getTotalScore() { return totalScore; }
    public String getGrade() { return grade; }
    public int getTrendScore() { return trendScore; }
    public int getTimeframeScore() { return timeframeScore; }
    public int getMomentumScore() { return momentumScore; }
    public int getVolatilityScore() { return volatilityScore; }
    public int getRiskScore() { return riskScore; }
    public List<String> getStrengths() { return strengths; }
    public List<String> getWeaknesses() { return weaknesses; }
}