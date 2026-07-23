package com.orbistrade.app.domain.assistant;

import java.util.Collections;
import java.util.List;

public final class ConfirmationReview {
    public enum Verdict {
        CONFIRMED,
        CONFLICT,
        INCONCLUSIVE,
        BLOCKED
    }

    private final Verdict verdict;
    private final int alignmentScore;
    private final List<String> supportingEvidence;
    private final List<String> opposingEvidence;
    private final String conclusion;

    public ConfirmationReview(
            Verdict verdict,
            int alignmentScore,
            List<String> supportingEvidence,
            List<String> opposingEvidence,
            String conclusion
    ) {
        this.verdict = verdict;
        this.alignmentScore = Math.max(0, Math.min(100, alignmentScore));
        this.supportingEvidence = Collections.unmodifiableList(supportingEvidence);
        this.opposingEvidence = Collections.unmodifiableList(opposingEvidence);
        this.conclusion = conclusion;
    }

    public Verdict getVerdict() {
        return verdict;
    }

    public int getAlignmentScore() {
        return alignmentScore;
    }

    public List<String> getSupportingEvidence() {
        return supportingEvidence;
    }

    public List<String> getOpposingEvidence() {
        return opposingEvidence;
    }

    public String getConclusion() {
        return conclusion;
    }
}
