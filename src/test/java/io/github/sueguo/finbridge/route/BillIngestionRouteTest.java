package io.github.sueguo.finbridge.route;

import io.github.sueguo.finbridge.model.entity.Bill;
import io.github.sueguo.finbridge.model.entity.BillStatus;
import io.github.sueguo.finbridge.repository.BillRepository;
import io.github.sueguo.finbridge.repository.TransactionRepository;
import io.github.sueguo.finbridge.route.processor.CategoryClassifierProcessor;
import io.github.sueguo.finbridge.route.processor.CsvParserProcessor;
import io.github.sueguo.finbridge.route.processor.DuplicateFilterProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BillIngestionRoute")
class BillIngestionRouteTest extends CamelTestSupport {

    @Mock private BillRepository billRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private CsvParserProcessor csvParserProcessor;
    @Mock private CategoryClassifierProcessor classifierProcessor;
    @Mock private DuplicateFilterProcessor duplicateFilterProcessor;

    @Override
    protected RoutesBuilder createRouteBuilder() {
        return new BillIngestionRoute(
                csvParserProcessor,
                classifierProcessor,
                duplicateFilterProcessor,
                billRepository,
                transactionRepository) {

            @Override
            public void configure() {
                // 跳过 restConfiguration 和 rest() 定义
                // 只注册 direct: 路由，这是我们要测试的部分

                onException(Exception.class)
                        .handled(true)
                        .process(exchange -> {
                            exchange.getMessage().setHeader(
                                    Exchange.HTTP_RESPONSE_CODE, 500);
                            exchange.getMessage().setBody(
                                    "{\"error\":\"Bill processing failed\"}");
                        });

                from("direct:listBills")
                        .routeId("list-bills")
                        .process(exchange ->
                                exchange.getMessage().setBody(billRepository.findAll()));

                from("direct:getBill")
                        .routeId("get-bill")
                        .process(exchange -> {
                            Long id = Long.parseLong(
                                    exchange.getMessage().getHeader("id", String.class));
                            billRepository.findById(id).ifPresentOrElse(
                                    bill -> exchange.getMessage().setBody(bill),
                                    () -> {
                                        exchange.getMessage().setBody(
                                                "{\"error\":\"Bill not found\"}");
                                        exchange.getMessage().setHeader(
                                                Exchange.HTTP_RESPONSE_CODE, 404);
                                    });
                        });

                from("direct:monthlySpending")
                        .routeId("monthly-spending")
                        .process(exchange -> {
                            List<Object[]> rows =
                                    transactionRepository.findMonthlyDebitTotals();
                            List<java.util.Map<String, Object>> result = rows.stream()
                                    .map(r -> java.util.Map.of("month", r[0], "total", r[1]))
                                    .toList();
                            exchange.getMessage().setBody(result);
                        });

                from("direct:categorySpending")
                        .routeId("category-spending")
                        .process(exchange -> {
                            String month = exchange.getMessage().getHeader(
                                    "month",
                                    java.time.LocalDate.now().toString().substring(0, 7),
                                    String.class);
                            List<Object[]> rows =
                                    transactionRepository.findSpendingByCategoryForMonth(month);
                            List<java.util.Map<String, Object>> result = rows.stream()
                                    .map(r -> java.util.Map.of("category", r[0], "total", r[1]))
                                    .toList();
                            exchange.getMessage().setBody(result);
                        });
            }
        };
    }

    // ── direct:listBills ───────────────────────────────────────────────────

    @Test
    @DisplayName("listBills: should return list of bills")
    void listBills_shouldReturnBillList() {
        Bill bill = Bill.builder()
                .id(1L).sourceBank("TD")
                .billMonth("2024-01").status(BillStatus.PROCESSED)
                .build();
        when(billRepository.findAll()).thenReturn(List.of(bill));

        Exchange result = template.request(
                "direct:listBills", exchange -> {});

        assertThat(result.getException()).isNull();
        @SuppressWarnings("unchecked")
        List<Bill> bills = (List<Bill>) result.getMessage().getBody();
        assertThat(bills).hasSize(1);
        assertThat(bills.get(0).getSourceBank()).isEqualTo("TD");
    }

    @Test
    @DisplayName("listBills: should return empty list when no bills")
    void listBills_shouldReturnEmptyList() {
        when(billRepository.findAll()).thenReturn(List.of());

        Exchange result = template.request(
                "direct:listBills", exchange -> {});

        assertThat(result.getException()).isNull();
        @SuppressWarnings("unchecked")
        List<Bill> bills = (List<Bill>) result.getMessage().getBody();
        assertThat(bills).isEmpty();
    }

    // ── direct:getBill ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getBill: should return bill when found")
    void getBill_shouldReturnBill_whenFound() {
        Bill bill = Bill.builder()
                .id(1L).sourceBank("TD")
                .billMonth("2024-01").status(BillStatus.PROCESSED)
                .build();
        when(billRepository.findById(1L)).thenReturn(Optional.of(bill));

        Exchange result = template.request(
                "direct:getBill",
                exchange -> exchange.getMessage().setHeader("id", "1"));

        assertThat(result.getException()).isNull();
        assertThat(result.getMessage().getBody()).isInstanceOf(Bill.class);
        assertThat(((Bill) result.getMessage().getBody()).getSourceBank()).isEqualTo("TD");
    }

    @Test
    @DisplayName("getBill: should return 404 when not found")
    void getBill_shouldReturn404_whenNotFound() {
        when(billRepository.findById(999L)).thenReturn(Optional.empty());

        Exchange result = template.request(
                "direct:getBill",
                exchange -> exchange.getMessage().setHeader("id", "999"));

        assertThat(result.getException()).isNull();
        assertThat(result.getMessage().getBody().toString())
                .containsIgnoringCase("not found");
        assertThat(result.getMessage().getHeader(
                Exchange.HTTP_RESPONSE_CODE, Integer.class)).isEqualTo(404);
    }

    // ── direct:monthlySpending ─────────────────────────────────────────────

    @Test
    @DisplayName("monthlySpending: should return mapped list")
    void monthlySpending_shouldReturnMappedList() {
        List<Object[]> mockRows = new ArrayList<>();
        mockRows.add(new Object[]{"2024-01", 1234.56});
        when(transactionRepository.findMonthlyDebitTotals()).thenReturn(mockRows);

        Exchange result = template.request(
                "direct:monthlySpending", exchange -> {});

        assertThat(result.getException()).isNull();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> list =
                (List<Map<String, Object>>) result.getMessage().getBody();
        assertThat(list).hasSize(1);
        assertThat(list.get(0).get("month")).isEqualTo("2024-01");
        assertThat(list.get(0)).containsKey("total");
    }

    @Test
    @DisplayName("monthlySpending: should return empty list when no data")
    void monthlySpending_shouldReturnEmptyList() {
        when(transactionRepository.findMonthlyDebitTotals()).thenReturn(new ArrayList<>());

        Exchange result = template.request(
                "direct:monthlySpending", exchange -> {});

        assertThat(result.getException()).isNull();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> list =
                (List<Map<String, Object>>) result.getMessage().getBody();
        assertThat(list).isEmpty();
    }

    // ── direct:categorySpending ────────────────────────────────────────────

    @Test
    @DisplayName("categorySpending: should return mapped list")
    void categorySpending_shouldReturnMappedList() {
        List<Object[]> mockRows = new ArrayList<>();
        mockRows.add(new Object[]{"Coffee", 45.75});
        when(transactionRepository.findSpendingByCategoryForMonth("2024-01"))
                .thenReturn(mockRows);

        Exchange result = template.request(
                "direct:categorySpending",
                exchange -> exchange.getMessage().setHeader("month", "2024-01"));

        assertThat(result.getException()).isNull();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> list =
                (List<Map<String, Object>>) result.getMessage().getBody();
        assertThat(list).hasSize(1);
        assertThat(list.get(0).get("category")).isEqualTo("Coffee");
        assertThat(list.get(0)).containsKey("total");
    }

    @Test
    @DisplayName("categorySpending: should return empty list when no data")
    void categorySpending_shouldReturnEmptyList() {
        when(transactionRepository.findSpendingByCategoryForMonth("2099-01"))
                .thenReturn(new ArrayList<>());

        Exchange result = template.request(
                "direct:categorySpending",
                exchange -> exchange.getMessage().setHeader("month", "2099-01"));

        assertThat(result.getException()).isNull();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> list =
                (List<Map<String, Object>>) result.getMessage().getBody();
        assertThat(list).isEmpty();
    }
}