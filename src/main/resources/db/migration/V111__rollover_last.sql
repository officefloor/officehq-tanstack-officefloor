-- The sheet's last rollover, kept so it can be undone: who made it, on which sheet-today, for which fiscal year,
-- the periods it generated and the opening balances it added. Replaced by each rollover; dropped when undone.
CREATE TABLE rollover_last (
    sheet_id VARCHAR(200) NOT NULL PRIMARY KEY REFERENCES sheet (id),
    by_user  VARCHAR(200) NOT NULL,
    made_on  DATE,
    fy       VARCHAR(20)  NOT NULL
);
CREATE TABLE rollover_last_period (
    sheet_id  VARCHAR(200) NOT NULL REFERENCES sheet (id),
    period_id BIGINT       NOT NULL,
    PRIMARY KEY (sheet_id, period_id)
);
CREATE TABLE rollover_last_opening (
    sheet_id  VARCHAR(200)   NOT NULL REFERENCES sheet (id),
    period_id BIGINT         NOT NULL,
    person_id BIGINT         NOT NULL,
    amount    DECIMAL(19, 2) NOT NULL,
    PRIMARY KEY (sheet_id, period_id, person_id)
);
