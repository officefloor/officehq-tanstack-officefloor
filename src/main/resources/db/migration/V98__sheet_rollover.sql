-- A sheet's last rollover, kept so its steward can undo it on the same sheet-today: the fiscal year rolled
-- over, who made it and on which sheet-today, and the periods it generated (names, comma separated). A later
-- rollover replaces it; undoing it removes it.
CREATE TABLE sheet_rollover (
    sheet_id VARCHAR(200)  NOT NULL PRIMARY KEY,
    fy       VARCHAR(10)   NOT NULL,
    made_by  VARCHAR(255)  NOT NULL,
    made_on  DATE          NOT NULL,
    periods  VARCHAR(4000) NOT NULL
);

-- Each opening balance the sheet's last rollover set: the amount it added to the person's opening balance
-- for the period, and whether that opening balance was new (made by the rollover).
CREATE TABLE sheet_rollover_opening (
    sheet_id  VARCHAR(200)   NOT NULL,
    period    VARCHAR(200)   NOT NULL,
    person_id BIGINT         NOT NULL,
    amount    DECIMAL(19, 2) NOT NULL,
    created   BOOLEAN        NOT NULL,
    PRIMARY KEY (sheet_id, period, person_id)
);
