package io.github.sueguo.finbridge.route.processor;

import io.github.sueguo.finbridge.model.entity.Transaction;
import io.github.sueguo.finbridge.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Removes duplicate transactions before DB insertion.
 * Deduplication key: bill_id + date + amount + raw_description
 * (mirrors the database unique constraint).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DuplicateFilterProcessor implements Processor {

    private final TransactionRepository transactionRepository;

    @Override
    @SuppressWarnings("unchecked")
    public void process(Exchange exchange) {
        List<Transaction> transactions = exchange.getMessage().getBody(List.class);
        if (transactions == null) return;

        Long billId = exchange.getMessage().getHeader("billId", Long.class);

        List<Transaction> existing = billId != null
                ? transactionRepository.findByBillId(billId)
                : List.of();

        List<Transaction> filtered = transactions.stream()
                .filter(tx -> existing.stream().noneMatch(ex -> isDuplicate(tx, ex)))
                .toList();

        int dropped = transactions.size() - filtered.size();
        if (dropped > 0) {
            log.warn("Dropped {} duplicate transactions for billId={}", dropped, billId);
        }

        exchange.getMessage().setBody(filtered);
    }

    private boolean isDuplicate(Transaction a, Transaction b) {
        return Objects.equals(a.getTransactionDate(), b.getTransactionDate())
                && Objects.equals(a.getAmount(), b.getAmount())
                && Objects.equals(a.getRawDescription(), b.getRawDescription());
    }
}
