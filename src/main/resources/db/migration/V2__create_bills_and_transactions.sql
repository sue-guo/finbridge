-- V2: Bills and Transactions tables

CREATE TABLE bills (
    id                BIGSERIAL PRIMARY KEY,
    source_bank       VARCHAR(60)  NOT NULL,
    bill_month        VARCHAR(7)   NOT NULL,     -- format: YYYY-MM
    original_filename VARCHAR(255) NOT NULL,
    s3_key            VARCHAR(512),
    status            VARCHAR(20)  NOT NULL DEFAULT 'UPLOADED',
    error_message     TEXT,
    uploaded_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    processed_at      TIMESTAMPTZ,
    CONSTRAINT uq_bill_source_month UNIQUE (source_bank, bill_month)
);

CREATE TABLE transactions (
    id                      BIGSERIAL PRIMARY KEY,
    bill_id                 BIGINT       NOT NULL REFERENCES bills(id) ON DELETE CASCADE,
    transaction_date        DATE         NOT NULL,
    raw_description         VARCHAR(255) NOT NULL,
    normalized_description  VARCHAR(255),
    amount                  NUMERIC(12,2) NOT NULL,
    currency                VARCHAR(3)   NOT NULL DEFAULT 'CAD',
    transaction_type        VARCHAR(10)  NOT NULL,   -- DEBIT or CREDIT
    category_id             BIGINT REFERENCES categories(id),
    expense_type            VARCHAR(20),             -- RECURRING or FLEXIBLE
    is_recurring            BOOLEAN DEFAULT FALSE,
    notes                   VARCHAR(500),
    CONSTRAINT uq_transaction_dedup UNIQUE (bill_id, transaction_date, amount, raw_description)
);

CREATE INDEX idx_tx_date     ON transactions(transaction_date);
CREATE INDEX idx_tx_category ON transactions(category_id);
CREATE INDEX idx_tx_bill     ON transactions(bill_id);
-- CREATE INDEX idx_tx_month    ON transactions(DATE_TRUNC('month', transaction_date));

-- Power BI convenience view: monthly spending summary
CREATE VIEW v_monthly_spending AS
SELECT
    TO_CHAR(t.transaction_date, 'YYYY-MM')  AS month,
    c.display_name                           AS category,
    c2.display_name                          AS parent_category,
    t.expense_type,
    SUM(t.amount)                            AS total_amount,
    COUNT(*)                                 AS transaction_count
FROM transactions t
LEFT JOIN categories c  ON t.category_id = c.id
LEFT JOIN categories c2 ON c.parent_id   = c2.id
WHERE t.transaction_type = 'DEBIT'
GROUP BY 1, 2, 3, 4;

-- Power BI convenience view: annual comparison
CREATE VIEW v_annual_comparison AS
SELECT
    EXTRACT(YEAR FROM transaction_date)  AS year,
    EXTRACT(MONTH FROM transaction_date) AS month,
    c.display_name                        AS category,
    SUM(amount)                           AS total
FROM transactions t
LEFT JOIN categories c ON t.category_id = c.id
WHERE transaction_type = 'DEBIT'
GROUP BY 1, 2, 3;
