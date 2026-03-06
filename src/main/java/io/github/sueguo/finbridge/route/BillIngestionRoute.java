package io.github.sueguo.finbridge.route;


import io.github.sueguo.finbridge.model.entity.Bill;
import io.github.sueguo.finbridge.model.entity.BillStatus;
import io.github.sueguo.finbridge.model.entity.Transaction;
import io.github.sueguo.finbridge.repository.BillRepository;
import io.github.sueguo.finbridge.repository.TransactionRepository;
import io.github.sueguo.finbridge.route.processor.CategoryClassifierProcessor;
import io.github.sueguo.finbridge.route.processor.CsvParserProcessor;
import io.github.sueguo.finbridge.route.processor.DuplicateFilterProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Camel route definitions for the Bill Manager.
 *
 * REST endpoints:
 *   POST /api/bills/upload   - upload a CSV bill file for processing
 *   GET  /api/bills          - list all bills
 *   GET  /api/bills/{id}     - get bill detail
 *   GET  /api/analytics/monthly-spending  - monthly debit totals
 *   GET  /api/analytics/category-spending - category breakdown for a month
 *
 * Ingestion pipeline (direct:ingestBill):
 *   parse CSV -> classify categories -> filter duplicates -> persist to DB
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BillIngestionRoute extends RouteBuilder {

    private final CsvParserProcessor csvParserProcessor;
    private final CategoryClassifierProcessor classifierProcessor;
    private final DuplicateFilterProcessor duplicateFilterProcessor;
    private final BillRepository billRepository;
    private final TransactionRepository transactionRepository;

    @Override
    public void configure() {

        // ── Global error handler ───────────────────────────────────────────
        onException(Exception.class)
                .handled(true)
                .log("Error in bill ingestion: ${exception.message}")
                .process(exchange -> {
                    Long billId = exchange.getMessage().getHeader("billId", Long.class);
                    if (billId != null) {
                        billRepository.findById(billId).ifPresent(bill -> {
                            bill.setStatus(BillStatus.ERROR);
                            bill.setErrorMessage(exchange.getProperty(Exchange.EXCEPTION_CAUGHT,
                                    Exception.class).getMessage());
                            billRepository.save(bill);
                        });
                    }
                    exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, 500);
                    exchange.getMessage().setBody("{\"error\":\"Bill processing failed\"}");
                });

        // ── REST DSL config ────────────────────────────────────────────────
        restConfiguration()
                .component("platform-http")
                .bindingMode(RestBindingMode.json)
                .dataFormatProperty("prettyPrint", "true")
                .enableCORS(true)
                .contextPath("/api");

        // ── REST: Upload bill ──────────────────────────────────────────────
        rest("/bills")
                .post("/upload")
                .description("Upload a CSV bill file for ingestion")
                .consumes(MediaType.MULTIPART_FORM_DATA_VALUE)
                .produces(MediaType.APPLICATION_JSON_VALUE)
                .to("direct:uploadBill")
                .get()
                .description("List all bills")
                .produces(MediaType.APPLICATION_JSON_VALUE)
                .to("direct:listBills")
                .get("/{id}")
                .description("Get bill details and transactions")
                .produces(MediaType.APPLICATION_JSON_VALUE)
                .to("direct:getBill");

        // ── REST: Analytics ───────────────────────────────────────────────
        rest("/analytics")
                .get("/monthly-spending")
                .description("Monthly debit totals (for trend chart)")
                .produces(MediaType.APPLICATION_JSON_VALUE)
                .to("direct:monthlySpending")
                .get("/category-spending")
                .description("Category breakdown. Query param: month=2024-11")
                .produces(MediaType.APPLICATION_JSON_VALUE)
                .to("direct:categorySpending");

        // ── Route: upload bill ─────────────────────────────────────────────
        from("direct:uploadBill")
                .routeId("upload-bill")
                .log("Received bill upload: bank=${header.sourceBank}, month=${header.billMonth}")
                .process(exchange -> {
                    String sourceBank = exchange.getMessage().getHeader("sourceBank", String.class);
                    String billMonth  = exchange.getMessage().getHeader("billMonth", String.class);
                    String filename   = exchange.getMessage().getHeader("filename", "upload.csv", String.class);

                    Bill bill = Bill.builder()
                            .sourceBank(sourceBank)
                            .billMonth(billMonth)
                            .originalFilename(filename)
                            .status(BillStatus.UPLOADED)
                            .build();
                    bill = billRepository.save(bill);

                    exchange.getMessage().setHeader("billId", bill.getId());
                    exchange.getMessage().setBody(bill);
                })
                .to("direct:ingestBill");

        // ── Route: ingestion pipeline ──────────────────────────────────────
        from("direct:ingestBill")
                .routeId("ingest-bill")
                .log("Starting ingestion for billId=${header.billId}")
                .process(exchange -> {
                    Long billId = exchange.getMessage().getHeader("billId", Long.class);
                    billRepository.findById(billId).ifPresent(b -> {
                        b.setStatus(BillStatus.PROCESSING);
                        billRepository.save(b);
                    });
                })
                .process(csvParserProcessor)
                .process(classifierProcessor)
                .process(duplicateFilterProcessor)
                .process(exchange -> {
                    // Persist transactions
                    Long billId = exchange.getMessage().getHeader("billId", Long.class);
                    Bill bill   = billRepository.findById(billId).orElseThrow();

                    @SuppressWarnings("unchecked")
                    List<Transaction> transactions = exchange.getMessage().getBody(List.class);
                    transactions.forEach(tx -> tx.setBill(bill));
                    transactionRepository.saveAll(transactions);

                    bill.setStatus(BillStatus.PROCESSED);
                    bill.setProcessedAt(Instant.now());
                    billRepository.save(bill);

                    log.info("Ingestion complete: billId={}, saved {} transactions",
                            billId, transactions.size());
                    exchange.getMessage().setBody(bill);
                    exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, 201);
                });

        // ── Route: list bills ──────────────────────────────────────────────
        from("direct:listBills")
                .routeId("list-bills")
                .process(exchange -> exchange.getMessage().setBody(billRepository.findAll()));

        // ── Route: get bill ────────────────────────────────────────────────
        from("direct:getBill")
                .routeId("get-bill")
                .process(exchange -> {
                    Long id = Long.parseLong(
                            exchange.getMessage().getHeader("id", String.class));
                    billRepository.findById(id).ifPresentOrElse(
                            bill -> exchange.getMessage().setBody(bill),
                            () -> {
                                exchange.getMessage().setBody("{\"error\":\"Bill not found\"}");
                                exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, 404);
                            });
                });

        // ── Route: monthly spending analytics ─────────────────────────────
        from("direct:monthlySpending")
                .routeId("monthly-spending")
                .process(exchange -> {
                    List<Object[]> rows = transactionRepository.findMonthlyDebitTotals();
                    List<java.util.Map<String, Object>> result = rows.stream()
                            .map(r -> java.util.Map.of("month", r[0], "total", r[1]))
                            .toList();
                    exchange.getMessage().setBody(result);
                });

        // ── Route: category spending analytics ────────────────────────────
        from("direct:categorySpending")
                .routeId("category-spending")
                .process(exchange -> {
                    String month = exchange.getMessage().getHeader("month",
                            java.time.LocalDate.now().toString().substring(0, 7), String.class);
                    List<Object[]> rows = transactionRepository.findSpendingByCategoryForMonth(month);
                    List<java.util.Map<String, Object>> result = rows.stream()
                            .map(r -> java.util.Map.of("category", r[0], "total", r[1]))
                            .toList();
                    exchange.getMessage().setBody(result);
                });
    }
}
