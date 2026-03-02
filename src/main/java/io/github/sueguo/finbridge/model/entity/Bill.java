package io.github.sueguo.finbridge.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents one uploaded bank/credit-card statement file.
 * One Bill contains many Transactions.
 */
@Entity
@Table(name = "bills",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_bill_source_month",
                columnNames = {"source_bank", "bill_month"}))
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class Bill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_bank", nullable = false, length = 60)
    private String sourceBank;    // e.g. "TD", "RBC", "CIBC"

    @Column(name = "bill_month", nullable = false)
    private String billMonth;     // stored as "2024-11" (YearMonth ISO string)

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "s3_key")
    private String s3Key;         // S3 object key for the raw file

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private BillStatus status = BillStatus.UPLOADED;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Transaction> transactions = new ArrayList<>();
}

