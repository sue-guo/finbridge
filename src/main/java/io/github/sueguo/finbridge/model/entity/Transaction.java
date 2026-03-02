package io.github.sueguo.finbridge.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Single financial transaction parsed from a bank/credit-card CSV.
 * The unique constraint prevents duplicate imports.
 */
@Entity
@Table(name = "transactions",
        indexes = {
                @Index(name = "idx_tx_date",     columnList = "transaction_date"),
                @Index(name = "idx_tx_category", columnList = "category_id"),
                @Index(name = "idx_tx_bill",     columnList = "bill_id")
        },
        uniqueConstraints = @UniqueConstraint(
                name = "uq_transaction_dedup",
                columnNames = {"bill_id", "transaction_date", "amount", "raw_description"}))
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bill_id", nullable = false)
    private Bill bill;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "raw_description", nullable = false, length = 255)
    private String rawDescription;        // original text from CSV

    @Column(name = "normalized_description", length = 255)
    private String normalizedDescription; // cleaned / uppercase

    /**
     * Always stored as a positive value.
     * Use transactionType (DEBIT/CREDIT) to determine direction.
     */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "CAD";

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 10)
    private TransactionType transactionType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private Category category;          // null = uncategorized

    @Enumerated(EnumType.STRING)
    @Column(name = "expense_type", length = 20)
    private ExpenseType expenseType;    // RECURRING or FLEXIBLE

    @Column(name = "is_recurring")
    @Builder.Default
    private Boolean isRecurring = false;

    @Column(name = "notes", length = 500)
    private String notes;               // manual annotation
}

