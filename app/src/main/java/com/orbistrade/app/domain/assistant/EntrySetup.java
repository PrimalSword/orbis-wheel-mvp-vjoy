package com.orbistrade.app.domain.assistant;

public final class EntrySetup {
    public enum Status {
        READY,
        WAIT_TRIGGER,
        BLOCKED
    }

    private final Status status;
    private final String strategyName;
    private final int windowMinutes;
    private final double triggerPrice;
    private final double invalidationPrice;
    private final String triggerCondition;
    private final String warning;

    public EntrySetup(
            Status status,
            String strategyName,
            int windowMinutes,
            double triggerPrice,
            double invalidationPrice,
            String triggerCondition,
            String warning
    ) {
        this.status = status;
        this.strategyName = strategyName;
        this.windowMinutes = windowMinutes;
        this.triggerPrice = triggerPrice;
        this.invalidationPrice = invalidationPrice;
        this.triggerCondition = triggerCondition;
        this.warning = warning;
    }

    public Status getStatus() { return status; }
    public String getStrategyName() { return strategyName; }
    public int getWindowMinutes() { return windowMinutes; }
    public double getTriggerPrice() { return triggerPrice; }
    public double getInvalidationPrice() { return invalidationPrice; }
    public String getTriggerCondition() { return triggerCondition; }
    public String getWarning() { return warning; }
}