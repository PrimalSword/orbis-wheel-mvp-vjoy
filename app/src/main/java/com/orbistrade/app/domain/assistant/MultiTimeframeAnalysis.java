package com.orbistrade.app.domain.assistant;

public final class MultiTimeframeAnalysis {
    public enum Status {
        ALIGNED,
        CONFLICT,
        BLOCKED
    }

    private final Status status;
    private final int qualityScore;
    private final String contextSummary;
    private final String triggerSummary;
    private final String instruction;

    public MultiTimeframeAnalysis(
            Status status,
            int qualityScore,
            String contextSummary,
            String triggerSummary,
            String instruction
    ) {
        this.status = status;
        this.qualityScore = qualityScore;
        this.contextSummary = contextSummary;
        this.triggerSummary = triggerSummary;
        this.instruction = instruction;
    }

    public Status getStatus() {
        return status;
    }

    public int getQualityScore() {
        return qualityScore;
    }

    public String getContextSummary() {
        return contextSummary;
    }

    public String getTriggerSummary() {
        return triggerSummary;
    }

    public String getInstruction() {
        return instruction;
    }
}
