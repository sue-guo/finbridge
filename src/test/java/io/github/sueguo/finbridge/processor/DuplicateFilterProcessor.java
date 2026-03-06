package io.github.sueguo.finbridge.processor;

import io.github.sueguo.finbridge.model.entity.Transaction;
import io.github.sueguo.finbridge.model.entity.TransactionType;
import io.github.sueguo.finbridge.repository.TransactionRepository;
import io.github.sueguo.finbridge.route.processor.DuplicateFilterProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DuplicateFilterProcessor")
class DuplicateFilterProcessorTest {

    @Mock private TransactionRepository transactionRepository;
    private DuplicateFilterProcessor processor;
    private DefaultCamelContext camelContext;

    @BeforeEach
    void setUp() {
        processor = new DuplicateFilterProcessor(transactionRepository);
        camelContext = new DefaultCamelContext();
    }

    private Transaction buildTx(String desc, BigDecimal amount, LocalDate date) {
        return Transaction.builder()
                .rawDescription(desc)
                .amount(amount)
                .transactionDate(date)
                .transactionType(TransactionType.DEBIT)
                .currency("CAD")
                .build();
    }

    @Test
    @DisplayName("Should keep all transactions when no existing records")
    void shouldKeepAll_whenNoExisting() {
        when(transactionRepository.findByBillId(1L)).thenReturn(List.of());

        List<Transaction> incoming = List.of(
                buildTx("TIM HORTONS", new BigDecimal("5.75"), LocalDate.of(2024, 1, 15)),
                buildTx("LOBLAWS", new BigDecimal("87.32"), LocalDate.of(2024, 1, 16))
        );

        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getMessage().setHeader("billId", 1L);
        exchange.getMessage().setBody(incoming);

        processor.process(exchange);

        @SuppressWarnings("unchecked")
        List<Transaction> result = exchange.getMessage().getBody(List.class);
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("Should remove exact duplicate transactions")
    void shouldRemoveDuplicates() {
        LocalDate date = LocalDate.of(2024, 1, 15);
        BigDecimal amount = new BigDecimal("5.75");

        Transaction existing = buildTx("TIM HORTONS", amount, date);
        when(transactionRepository.findByBillId(1L)).thenReturn(List.of(existing));

        List<Transaction> incoming = new ArrayList<>();
        incoming.add(buildTx("TIM HORTONS", amount, date));   // duplicate
        incoming.add(buildTx("LOBLAWS", new BigDecimal("87.32"), date)); // new

        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getMessage().setHeader("billId", 1L);
        exchange.getMessage().setBody(incoming);

        processor.process(exchange);

        @SuppressWarnings("unchecked")
        List<Transaction> result = exchange.getMessage().getBody(List.class);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRawDescription()).isEqualTo("LOBLAWS");
    }

    @Test
    @DisplayName("Should keep all when no billId header")
    void shouldKeepAll_whenNoBillIdHeader() {
        List<Transaction> incoming = List.of(
                buildTx("TIM HORTONS", new BigDecimal("5.75"), LocalDate.of(2024, 1, 15))
        );

        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getMessage().setBody(incoming);

        processor.process(exchange);

        @SuppressWarnings("unchecked")
        List<Transaction> result = exchange.getMessage().getBody(List.class);
        assertThat(result).hasSize(1);
    }
}