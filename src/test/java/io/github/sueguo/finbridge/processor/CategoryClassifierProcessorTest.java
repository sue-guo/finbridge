package io.github.sueguo.finbridge.processor;

import io.github.sueguo.finbridge.model.entity.Category;
import io.github.sueguo.finbridge.model.entity.CategoryRule;
import io.github.sueguo.finbridge.model.entity.ExpenseType;
import io.github.sueguo.finbridge.model.entity.Transaction;
import io.github.sueguo.finbridge.repository.CategoryRuleRepository;
import io.github.sueguo.finbridge.route.processor.CategoryClassifierProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryClassifierProcessor")
class CategoryClassifierProcessorTest {

    @Mock private CategoryRuleRepository ruleRepository;
    private CategoryClassifierProcessor processor;
    private DefaultCamelContext camelContext;

    @BeforeEach
    void setUp() {
        Category coffeeCategory = Category.builder()
                .id(1L).name("COFFEE").displayName("Coffee")
                .expenseType(ExpenseType.FLEXIBLE).build();
        CategoryRule rule = CategoryRule.builder()
                .id(1L).keyword("TIM HORTON").category(coffeeCategory).priority(10).build();
        when(ruleRepository.findAllByOrderByPriorityAsc()).thenReturn(List.of(rule));
        processor = new CategoryClassifierProcessor(ruleRepository);
        processor.refreshRules();
        camelContext = new DefaultCamelContext();
    }

    @Test
    @DisplayName("Should classify matching transaction")
    void shouldClassifyMatchingTransaction() {
        Transaction tx = Transaction.builder().normalizedDescription("TIM HORTONS #456 OTTAWA").build();
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getMessage().setBody(List.of(tx));
        processor.process(exchange);
        assertThat(tx.getCategory().getName()).isEqualTo("COFFEE");
        assertThat(tx.getExpenseType()).isEqualTo(ExpenseType.FLEXIBLE);
    }

    @Test
    @DisplayName("Should leave uncategorized for unknown description")
    void shouldLeaveUnknownUncategorized() {
        Transaction tx = Transaction.builder().normalizedDescription("UNKNOWN XYZ").build();
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getMessage().setBody(List.of(tx));
        processor.process(exchange);
        assertThat(tx.getCategory()).isNull();
    }
}
