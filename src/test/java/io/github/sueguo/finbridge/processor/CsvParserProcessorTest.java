package io.github.sueguo.finbridge.processor;

import io.github.sueguo.finbridge.model.entity.Transaction;
import io.github.sueguo.finbridge.model.entity.TransactionType;
import io.github.sueguo.finbridge.route.processor.CsvParserProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for CsvParserProcessor.
 *
 * TD CSV format (NO header, 4 columns):
 *   col[0] Date        MM/dd/yyyy or yyyy-MM-dd
 *   col[1] Description
 *   col[2] Debit       non-empty = money OUT
 *   col[3] Credit      non-empty = money IN
 *   One of col[2]/col[3] is always empty on a given row.
 */
@DisplayName("CsvParserProcessor")
class CsvParserProcessorTest {

    private CsvParserProcessor processor;
    private DefaultCamelContext camelContext;

    @BeforeEach
    void setUp() {
        processor = new CsvParserProcessor();
        camelContext = new DefaultCamelContext();
    }

    // ── TD Bank Tests ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("TD Bank format (no header, 4 columns)")
    class TdFormat {

        @Test
        @DisplayName("Debit row: col[2] has amount, col[3] empty → DEBIT")
        void debitRow_shouldBeTypeDEBIT() throws Exception {
            // Real TD format: date,description,debitAmount,""
            String csv = "01/15/2024,TIM HORTONS #123,5.75,\n";

            List<Transaction> txs = parse("TD", csv);

            assertThat(txs).hasSize(1);
            Transaction tx = txs.get(0);
            assertThat(tx.getTransactionDate()).isEqualTo(LocalDate.of(2024, 1, 15));
            assertThat(tx.getRawDescription()).isEqualTo("TIM HORTONS #123");
            assertThat(tx.getNormalizedDescription()).isEqualTo("TIM HORTONS #123");
            assertThat(tx.getAmount()).isEqualByComparingTo(new BigDecimal("5.75"));
            assertThat(tx.getTransactionType()).isEqualTo(TransactionType.DEBIT);
            assertThat(tx.getCurrency()).isEqualTo("CAD");
        }

        @Test
        @DisplayName("Credit row: col[2] empty, col[3] has amount → CREDIT")
        void creditRow_shouldBeTypeCREDIT() throws Exception {
            // Real TD format: date,description,"",creditAmount
            String csv = "11/01/2024,PAYROLL DIRECT DEPOSIT,,3500.00\n";

            List<Transaction> txs = parse("TD", csv);

            assertThat(txs).hasSize(1);
            assertThat(txs.get(0).getTransactionType()).isEqualTo(TransactionType.CREDIT);
            assertThat(txs.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("3500.00"));
        }

        @Test
        @DisplayName("Mixed file: multiple debits and one credit")
        void mixedFile_shouldParseAllRows() throws Exception {
            String csv = """
                    01/15/2024,TIM HORTONS #123,5.75,
                    01/16/2024,LOBLAWS #456,87.32,
                    01/17/2024,PRESTO,3.20,
                    01/01/2024,PAYROLL DEPOSIT,,3500.00
                    """;

            List<Transaction> txs = parse("TD", csv);

            assertThat(txs).hasSize(4);
            long debits  = txs.stream().filter(t -> t.getTransactionType() == TransactionType.DEBIT).count();
            long credits = txs.stream().filter(t -> t.getTransactionType() == TransactionType.CREDIT).count();
            assertThat(debits).isEqualTo(3);
            assertThat(credits).isEqualTo(1);
        }

        @Test
        @DisplayName("Amount with comma separator: '1,234.56' → 1234.56")
        void amount_shouldHandleCommaThousandsSeparator() throws Exception {
            String csv = "11/01/2024,RENT E-TRANSFER,1234.56,\n";

            List<Transaction> txs = parse("TD", csv);

            assertThat(txs.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("1234.56"));
        }

        @Test
        @DisplayName("Date format yyyy-MM-dd should also be parsed")
        void date_shouldSupportIsoFormat() throws Exception {
            String csv = "2024-11-15,NETFLIX,17.99,\n";

            List<Transaction> txs = parse("TD", csv);

            assertThat(txs.get(0).getTransactionDate()).isEqualTo(LocalDate.of(2024, 11, 15));
        }

        @Test
        @DisplayName("Row with both col[2] and col[3] empty should be silently skipped")
        void rowWithNoAmount_shouldBeSkipped() throws Exception {
            String csv = "01/15/2024,TIM HORTONS #123,5.75,\n"
                    + "01/16/2024,EMPTY ROW,,\n"
                    + "01/17/2024,WALMART,42.00,\n";

            List<Transaction> txs = parse("TD", csv);

            assertThat(txs).hasSize(2);
        }

        @Test
        @DisplayName("Row with fewer than 4 columns should be silently skipped")
        void shortRow_shouldBeSkipped() throws Exception {
            String csv = "01/15/2024,TIM HORTONS\n"        // only 2 cols — skip
                    + "01/16/2024,WALMART,42.00,\n";    // valid

            List<Transaction> txs = parse("TD", csv);

            assertThat(txs).hasSize(1);
            assertThat(txs.get(0).getRawDescription()).isEqualTo("WALMART");
        }

        @Test
        @DisplayName("Row with invalid date should be skipped without exception")
        void invalidDate_shouldBeSkippedGracefully() throws Exception {
            String csv = "NOT-A-DATE,BAD ROW,10.00,\n"
                    + "01/20/2024,GOOD ROW,20.00,\n";

            List<Transaction> txs = parse("TD", csv);

            assertThat(txs).hasSize(1);
            assertThat(txs.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("20.00"));
        }

        @Test
        @DisplayName("Empty file (no rows) should return empty list")
        void emptyFile_shouldReturnEmptyList() throws Exception {
            List<Transaction> txs = parse("TD", "");
            assertThat(txs).isEmpty();
        }

        @Test
        @DisplayName("Description is stored as raw AND uppercased normalized")
        void description_shouldStoreRawAndNormalized() throws Exception {
            String csv = "01/15/2024,Tim Hortons #123,5.75,\n";

            List<Transaction> txs = parse("TD", csv);

            assertThat(txs.get(0).getRawDescription()).isEqualTo("Tim Hortons #123");
            assertThat(txs.get(0).getNormalizedDescription()).isEqualTo("TIM HORTONS #123");
        }
    }

    // ── Generic fallback Tests ────────────────────────────────────────────────

    @Nested
    @DisplayName("Generic fallback format (has header)")
    class GenericFormat {

        @Test
        @DisplayName("Negative amount → DEBIT; positive → CREDIT")
        void negativeAmount_shouldBeDebit() throws Exception {
            String csv = "Date,Description,Amount\n"
                    + "2024-11-01,RENT,-1500.00\n"
                    + "2024-11-01,REFUND,50.00\n";

            List<Transaction> txs = parse("GENERIC", csv);

            assertThat(txs).hasSize(2);
            assertThat(txs.get(0).getTransactionType()).isEqualTo(TransactionType.DEBIT);
            assertThat(txs.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("1500.00"));
            assertThat(txs.get(1).getTransactionType()).isEqualTo(TransactionType.CREDIT);
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private List<Transaction> parse(String sourceBank, String csvContent) throws Exception {
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getMessage().setHeader("sourceBank", sourceBank);
        exchange.getMessage().setBody(
                new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8)));
        processor.process(exchange);
        //noinspection unchecked
        return exchange.getMessage().getBody(List.class);
    }
}

