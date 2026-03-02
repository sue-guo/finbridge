package io.github.sueguo.finbridge.model.entity;

/** Whether a transaction is a predictable recurring charge or variable spend. */
public enum ExpenseType {
    RECURRING,  // rent, subscriptions, insurance
    FLEXIBLE    // dining, shopping, entertainment
}

