-- Driver-based forecasting: scenarios, one forecast method per account/department/year, and driver values per scenario.

CREATE TABLE scenario (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code        VARCHAR(10)  NOT NULL UNIQUE,
    name        VARCHAR(50)  NOT NULL,
    description VARCHAR(255) NOT NULL,
    sort_order  INTEGER      NOT NULL
);

-- The three scenarios are fixed reference data that every installation needs, so they live in the migration.
INSERT INTO scenario (code, name, description, sort_order) VALUES
    ('BASE',  'Base',  'Most likely outcome given current trends', 1),
    ('BEST',  'Best',  'Optimistic: drivers move in the company''s favour', 2),
    ('WORST', 'Worst', 'Pessimistic: drivers move against the company', 3);

-- How one account in one department is forecast for a fiscal year. Lines without a row here use their budget.
CREATE TABLE forecast_line (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    fiscal_year    INTEGER     NOT NULL CHECK (fiscal_year BETWEEN 2000 AND 2100),
    account_id     BIGINT      NOT NULL REFERENCES account (id),
    department_id  BIGINT      NOT NULL REFERENCES department (id),
    method         VARCHAR(20) NOT NULL CHECK (method IN ('UNITS_X_RATE', 'PERCENT_OF_REVENUE', 'BUDGET')),
    quantity_label VARCHAR(60),
    rate_label     VARCHAR(60),
    CONSTRAINT uq_forecast_line_year_account_department UNIQUE (fiscal_year, account_id, department_id)
);

-- Driver values for one line in one scenario. Which columns are used depends on the line's method:
-- UNITS_X_RATE uses quantity and rate; PERCENT_OF_REVENUE uses percent (e.g. 7.6 means 7.6%); BUDGET uses none.
CREATE TABLE forecast_driver_value (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    forecast_line_id BIGINT         NOT NULL REFERENCES forecast_line (id) ON DELETE CASCADE,
    scenario_id      BIGINT         NOT NULL REFERENCES scenario (id),
    quantity         NUMERIC(12, 2) CHECK (quantity >= 0),
    rate             NUMERIC(15, 2) CHECK (rate >= 0),
    percent          NUMERIC(7, 4)  CHECK (percent BETWEEN 0 AND 100),
    CONSTRAINT uq_forecast_driver_value_line_scenario UNIQUE (forecast_line_id, scenario_id)
);

CREATE INDEX idx_forecast_line_year ON forecast_line (fiscal_year);
