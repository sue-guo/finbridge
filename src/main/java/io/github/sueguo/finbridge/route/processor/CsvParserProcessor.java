package io.github.sueguo.finbridge.route.processor;

import com.opencsv.CSVReader;
import io.github.sueguo.finbridge.model.entity.Transaction;
import io.github.sueguo.finbridge.model.entity.TransactionType;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses bank CSV files into a list of raw Transaction objects.
 * Supports TD, RBC, and a generic fallback format.
 * Source bank is read from Exchange header "sourceBank".
 *
 * <p><b>TD format — NO header row, exactly 4 columns:</b>
 * <pre>
 *   col[0]  Date         e.g. "01/15/2024"
 *   col[1]  Description  e.g. "TIM HORTONS #123"
 *   col[2]  Debit        e.g. "5.75"   non-empty = money OUT  (DEBIT)
 *   col[3]  Credit       e.g. ""       non-empty = money IN   (CREDIT)
 * </pre>
 * Exactly one of col[2] / col[3] is populated per row; the other is always empty.
 *
 * <p><b>RBC format:</b> AccountType, AccountNum, Date, ChequeNum, Desc1, Desc2, CAD, USD
 * <p><b>Generic fallback:</b> Date, Description, Amount, [Currency]
 */
@Slf4j
@Component
public class CsvParserProcessor implements Processor {

    private static final DateTimeFormatter DATE_PARSER = new DateTimeFormatterBuilder()
            .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            .appendOptional(DateTimeFormatter.ofPattern("MM/dd/yyyy"))
            .appendOptional(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            .toFormatter();

    @Override
    public void process(Exchange exchange) throws Exception {
        String sourceBank = exchange.getMessage().getHeader("sourceBank", "GENERIC", String.class);
        InputStream inputStream = exchange.getMessage().getBody(InputStream.class);

        List<Transaction> transactions = switch (sourceBank.toUpperCase()) {
            case "TD"  -> parseTd(inputStream);
            case "RBC" -> parseRbc(inputStream);
            default    -> parseGeneric(inputStream);
        };

        log.info("Parsed {} transactions from {} CSV", transactions.size(), sourceBank);
        exchange.getMessage().setBody(transactions);
    }

    // ── TD Bank ───────────────────────────────────────────────────────────────
    // NO header row. 4 columns. col[2]=debit (money out), col[3]=credit (money in).
    // Exactly one of the two amount columns is non-empty on any given row.

    private List<Transaction> parseTd(InputStream is) throws Exception {
        List<Transaction> result = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String[] row;
            while ((row = reader.readNext()) != null) {
                if (row.length < 4) {
                    log.debug("Skipping short TD row ({} cols): {}", row.length, String.join(",", row));
                    continue;
                }
                try {
                    LocalDate date     = LocalDate.parse(row[0].trim(), DATE_PARSER);
                    String description = row[1].trim();
                    String debitStr    = row[2].trim();   // spending / money out
                    String creditStr   = row[3].trim();   // income  / money in

                    final BigDecimal amount;
                    final TransactionType type;

                    if (!debitStr.isEmpty()) {
                        amount = parseMoney(debitStr);
                        type   = TransactionType.DEBIT;
                    } else if (!creditStr.isEmpty()) {
                        amount = parseMoney(creditStr);
                        type   = TransactionType.CREDIT;
                    } else {
                        log.warn("TD row has no amount in col[2] or col[3], skipping: {}",
                                String.join(",", row));
                        continue;
                    }

                    result.add(buildTransaction(date, description, amount, type, "CAD"));
                } catch (Exception e) {
                    log.warn("Skipping unparseable TD row [{}]: {}", String.join(",", row), e.getMessage());
                }
            }
        }
        return result;
    }

    // ── RBC Bank ──────────────────────────────────────────────────────────────

    private List<Transaction> parseRbc(InputStream is) throws Exception {
        List<Transaction> result = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            reader.readNext(); // RBC has a header row
            String[] row;
            while ((row = reader.readNext()) != null) {
                if (row.length < 7) continue;
                try {
                    LocalDate date       = LocalDate.parse(row[2].trim(), DATE_PARSER);
                    String description   = (row[4].trim() + " " + row[5].trim()).trim();
                    BigDecimal cadAmount = parseMoney(row[6].trim());
                    // RBC uses negative = debit, positive = credit
                    TransactionType type = cadAmount.compareTo(BigDecimal.ZERO) < 0
                            ? TransactionType.DEBIT : TransactionType.CREDIT;
                    result.add(buildTransaction(date, description, cadAmount.abs(), type, "CAD"));
                } catch (Exception e) {
                    log.warn("Skipping RBC row: {}", String.join(",", row));
                }
            }
        }
        return result;
    }

    // ── Generic fallback ─────────────────────────────────────────────────────

    private List<Transaction> parseGeneric(InputStream is) throws Exception {
        List<Transaction> result = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            reader.readNext(); // assume header row
            String[] row;
            while ((row = reader.readNext()) != null) {
                if (row.length < 3) continue;
                try {
                    LocalDate date     = LocalDate.parse(row[0].trim(), DATE_PARSER);
                    String description = row[1].trim();
                    BigDecimal amount  = parseMoney(row[2].trim());
                    String currency    = row.length > 3 ? row[3].trim() : "CAD";
                    TransactionType type = amount.compareTo(BigDecimal.ZERO) < 0
                            ? TransactionType.DEBIT : TransactionType.CREDIT;
                    result.add(buildTransaction(date, description, amount.abs(), type, currency));
                } catch (Exception e) {
                    log.warn("Skipping generic row: {}", String.join(",", row));
                }
            }
        }
        return result;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Transaction buildTransaction(LocalDate date, String desc,
                                         BigDecimal amount, TransactionType type, String currency) {
        return Transaction.builder()
                .transactionDate(date)
                .rawDescription(desc)
                .normalizedDescription(desc.toUpperCase())
                .amount(amount)
                .currency(currency)
                .transactionType(type)
                .build();
    }

    private BigDecimal parseMoney(String value) {
        if (value == null || value.isBlank()) return BigDecimal.ZERO;
        // handles: "1,234.56", "$5.75", " 42.00 "
        return new BigDecimal(value.replace(",", "").replace("$", "").trim());
    }
}
