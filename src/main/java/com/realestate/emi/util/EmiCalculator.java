package com.realestate.emi.util;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class EmiCalculator {

    /**
     * Calculates the monthly EMI amount.
     *
     * If interestRatePercent == 0: EMI = principal / tenure
     * Else: EMI = P * r * (1+r)^n / ((1+r)^n - 1)
     *       where r = interestRatePercent / 12 / 100, n = tenure
     * Rounded to 2 decimal places HALF_UP.
     *
     * @param principal          the principal amount (totalPayableAfterDeposit)
     * @param annualInterestRate annual interest rate in percentage
     * @param tenureMonths       EMI tenure in months
     * @return computed EMI amount
     */
    public BigDecimal calculate(BigDecimal principal, BigDecimal annualInterestRate, int tenureMonths) {
        if (annualInterestRate == null || annualInterestRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(tenureMonths), 2, RoundingMode.HALF_UP);
        }
        double p = principal.doubleValue();
        double r = annualInterestRate.doubleValue() / 12.0 / 100.0;
        double n = tenureMonths;
        double emi = (p * r * Math.pow(1 + r, n)) / (Math.pow(1 + r, n) - 1);
        return BigDecimal.valueOf(emi).setScale(2, RoundingMode.HALF_UP);
    }
}
