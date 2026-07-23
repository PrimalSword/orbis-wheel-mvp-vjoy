package com.orbistrade.app.domain.risk;

public final class RiskProfile {
    private final double accountBalance;
    private final double riskPercent;
    private final double rewardRiskRatio;
    private final double atrStopMultiplier;

    public RiskProfile(
            double accountBalance,
            double riskPercent,
            double rewardRiskRatio,
            double atrStopMultiplier
    ) {
        if (accountBalance <= 0) {
            throw new IllegalArgumentException("O saldo deve ser maior que zero.");
        }
        if (riskPercent <= 0 || riskPercent > 5) {
            throw new IllegalArgumentException("O risco deve estar entre 0 e 5%.");
        }
        if (rewardRiskRatio < 1) {
            throw new IllegalArgumentException("A relação retorno/risco deve ser pelo menos 1.");
        }
        if (atrStopMultiplier <= 0) {
            throw new IllegalArgumentException("O multiplicador de ATR deve ser positivo.");
        }

        this.accountBalance = accountBalance;
        this.riskPercent = riskPercent;
        this.rewardRiskRatio = rewardRiskRatio;
        this.atrStopMultiplier = atrStopMultiplier;
    }

    public double getAccountBalance() {
        return accountBalance;
    }

    public double getRiskPercent() {
        return riskPercent;
    }

    public double getRewardRiskRatio() {
        return rewardRiskRatio;
    }

    public double getAtrStopMultiplier() {
        return atrStopMultiplier;
    }
}