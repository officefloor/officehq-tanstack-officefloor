-- Opening balances pasted for a person and a period: each paste line adds its amount, so a person's
-- opening for a period is the sum of their rows for it. It is part of their carried figure for it.
CREATE TABLE period_opening (
    id          BIGINT        AUTO_INCREMENT PRIMARY KEY,
    sheet_id    VARCHAR(200)  NOT NULL,
    period_name VARCHAR(200)  NOT NULL,
    person_id   BIGINT        NOT NULL,
    amount      DECIMAL(14,2) NOT NULL
);
CREATE INDEX period_opening_sheet ON period_opening (sheet_id, period_name, person_id);
