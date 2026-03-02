package io.github.sueguo.finbridge.model.entity;

public enum BillStatus {
    UPLOADED,    // file received, not yet processed
    PROCESSING,  // pipeline running
    PROCESSED,   // all transactions saved
    ERROR        // parsing or DB error
}
