package com.uade.skillswap.domain.common;

public class InsufficientBalanceException extends BusinessRuleException {

    public InsufficientBalanceException(long balance, long required) {
        super("Insufficient balance: " + balance + " < " + required);
    }
}
