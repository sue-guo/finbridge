package io.github.sueguo.finbridge.route.processor;

import io.github.sueguo.finbridge.model.entity.CategoryRule;
import io.github.sueguo.finbridge.model.entity.ExpenseType;
import io.github.sueguo.finbridge.model.entity.Transaction;
import io.github.sueguo.finbridge.repository.CategoryRuleRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Classifies each Transaction's category by matching its normalizedDescription
 * against CategoryRules loaded from the database.
 *
 * Rules are cached at startup and refreshed on demand via refreshRules().
 * Priority: lower integer = higher priority (first match wins).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CategoryClassifierProcessor implements Processor {

    private final CategoryRuleRepository ruleRepository;
    private List<CategoryRule> rules;

    @PostConstruct
    public void refreshRules() {
        this.rules = ruleRepository.findAllByOrderByPriorityAsc();
        log.info("Loaded {} category classification rules", rules.size());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void process(Exchange exchange) {
        List<Transaction> transactions = exchange.getMessage().getBody(List.class);
        if (transactions == null) return;

        int classified = 0;
        for (Transaction tx : transactions) {
            String desc = tx.getNormalizedDescription();
            if (desc == null) continue;

            for (CategoryRule rule : rules) {
                if (desc.contains(rule.getKeyword().toUpperCase())) {
                    tx.setCategory(rule.getCategory());
                    tx.setExpenseType(rule.getCategory().getExpenseType());
                    tx.setIsRecurring(rule.getCategory().getExpenseType() == ExpenseType.RECURRING);
                    classified++;
                    break; // first match wins
                }
            }
        }

        log.info("Classified {}/{} transactions", classified, transactions.size());
        exchange.getMessage().setBody(transactions);
    }
}
