package io.github.sueguo.finbridge.model.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Intermediate object produced by the CSV parser processor.
 * Passed through the Camel pipeline before being persisted as a Transaction.
 */
@Data
@Builder
public class RawTransactionRow {

    private LocalDate transactionDate;
    private String rawDescription;
    private BigDecimal amount;       // always positive after normalization
    private String transactionType;  // "DEBIT" or "CREDIT"
    private String currency;
    private String fingerprint;
}
