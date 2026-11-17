-- Each rollover of a fiscal year, in the order made: the old fiscal year (FY<yyyy>), who made it and the sheet's today
-- it was made on, the ids of the periods it generated and of the opening balances it set (each joined by ","), and
-- whether it has been undone. Only the sheet's last rollover can be undone (see UndoRollover).
CREATE TABLE rollover (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id    VARCHAR(200)  NOT NULL,
    fy          VARCHAR(20)   NOT NULL,
    made_by     VARCHAR(200)  NOT NULL,
    made_on     DATE          NOT NULL,
    period_ids  VARCHAR(8000) NOT NULL,
    opening_ids VARCHAR(8000) NOT NULL,
    undone      BOOLEAN       DEFAULT FALSE NOT NULL
);
CREATE INDEX rollover_sheet ON rollover (sheet_id, id);
