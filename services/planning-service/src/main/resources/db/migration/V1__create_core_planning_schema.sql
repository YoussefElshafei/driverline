-- Core planning schema: chart of accounts, departments, and monthly budget/actual amounts.

CREATE TABLE department (
    id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code VARCHAR(20)  NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE account (
    id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code VARCHAR(10)  NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    -- The type decides whether a positive variance is favorable (revenue) or unfavorable (expense).
    type VARCHAR(10)  NOT NULL CHECK (type IN ('REVENUE', 'EXPENSE'))
);

-- Amounts are stored with their natural sign: revenue and expenses are both positive.
-- Negatives are allowed for credits and refunds. NUMERIC is exact, unlike floating-point types.
CREATE TABLE budget_line (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    account_id    BIGINT         NOT NULL REFERENCES account (id),
    department_id BIGINT         NOT NULL REFERENCES department (id),
    fiscal_year   INTEGER        NOT NULL CHECK (fiscal_year BETWEEN 2000 AND 2100),
    fiscal_month  INTEGER        NOT NULL CHECK (fiscal_month BETWEEN 1 AND 12),
    amount        NUMERIC(15, 2) NOT NULL,
    CONSTRAINT uq_budget_line_account_department_period
        UNIQUE (account_id, department_id, fiscal_year, fiscal_month)
);

CREATE TABLE actual_line (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    account_id    BIGINT         NOT NULL REFERENCES account (id),
    department_id BIGINT         NOT NULL REFERENCES department (id),
    fiscal_year   INTEGER        NOT NULL CHECK (fiscal_year BETWEEN 2000 AND 2100),
    fiscal_month  INTEGER        NOT NULL CHECK (fiscal_month BETWEEN 1 AND 12),
    amount        NUMERIC(15, 2) NOT NULL,
    CONSTRAINT uq_actual_line_account_department_period
        UNIQUE (account_id, department_id, fiscal_year, fiscal_month)
);

-- Variance reports always filter by period, so index it. The unique constraints already index account_id first.
CREATE INDEX idx_budget_line_period ON budget_line (fiscal_year, fiscal_month);
CREATE INDEX idx_actual_line_period ON actual_line (fiscal_year, fiscal_month);
CREATE INDEX idx_budget_line_department ON budget_line (department_id);
CREATE INDEX idx_actual_line_department ON actual_line (department_id);
