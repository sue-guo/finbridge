package io.github.sueguo.finbridge.repository;

import io.github.sueguo.finbridge.model.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByBillId(Long billId);

    /** Monthly spending totals for trend chart (DEBIT only). */
    @Query("""
            SELECT SUBSTRING(CAST(t.transactionDate AS string), 1, 7) AS month,
                   SUM(t.amount) AS total
            FROM Transaction t
            WHERE t.transactionType = 'DEBIT'
            GROUP BY month
            ORDER BY month
            """)
    List<Object[]> findMonthlyDebitTotals();

    /** Spending by category for a given year-month (e.g. '2024-11'). */
    @Query("""
            SELECT c.displayName, SUM(t.amount)
            FROM Transaction t
            JOIN t.category c
            WHERE t.transactionType = 'DEBIT'
              AND SUBSTRING(CAST(t.transactionDate AS string), 1, 7) = :month
            GROUP BY c.displayName
            ORDER BY SUM(t.amount) DESC
            """)
    List<Object[]> findSpendingByCategoryForMonth(@Param("month") String month);

    /** Total debit for a month — used by budget alert logic. */
    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM Transaction t
            WHERE t.transactionType = 'DEBIT'
              AND SUBSTRING(CAST(t.transactionDate AS string), 1, 7) = :month
            """)
    BigDecimal sumDebitsForMonth(@Param("month") String month);
}
