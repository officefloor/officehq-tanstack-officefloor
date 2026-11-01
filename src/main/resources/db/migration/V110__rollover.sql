-- The sheet's last rollover, kept so its steward can undo it on the sheet's today it was made: who
-- made it, on which today, the fiscal year it rolled over, the periods it generated (comma separated
-- names) and, in rollover_opening, the amount it added to each person's opening balance. An undo, or
-- the next rollover, replaces it.
CREATE TABLE rollover (
    sheet_id  VARCHAR(200)  PRIMARY KEY,
    fy        INT           NOT NULL,
    user_name VARCHAR(200)  NOT NULL,
    made_on   DATE          NOT NULL,
    periods   VARCHAR(4000) NOT NULL
);

CREATE TABLE rollover_opening (
    sheet_id    VARCHAR(200)  NOT NULL,
    period_name VARCHAR(200)  NOT NULL,
    person_id   BIGINT        NOT NULL,
    amount      DECIMAL(14,2) NOT NULL,
    PRIMARY KEY (sheet_id, period_name, person_id)
);
