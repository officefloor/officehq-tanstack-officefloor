-- Each rollover made on a sheet, so the last one can be undone: the old fiscal year, who made it and on which
-- sheet-today, and whether it has been undone. The periods it generated and the opening balances it set are kept
-- in rollover_period and rollover_opening.
CREATE TABLE rollover (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(255) NOT NULL,
    fy       INT          NOT NULL,
    made_by  VARCHAR(255) NOT NULL,
    made_on  DATE         NOT NULL,
    undone   BOOLEAN      DEFAULT FALSE NOT NULL
);
CREATE INDEX rollover_sheet ON rollover (sheet_id);
CREATE TABLE rollover_period (
    rollover_id BIGINT       NOT NULL,
    period_name VARCHAR(255) NOT NULL
);
CREATE TABLE rollover_opening (
    rollover_id BIGINT         NOT NULL,
    period_name VARCHAR(255)   NOT NULL,
    person_id   BIGINT         NOT NULL,
    amount      DECIMAL(19, 2) NOT NULL
);
