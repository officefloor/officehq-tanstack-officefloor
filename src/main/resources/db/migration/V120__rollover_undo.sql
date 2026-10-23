-- Each rollover made on a sheet, so the last one can be undone on the sheet-day it was made: the old fiscal year
-- (FY<yyyy>), who made it, the sheet's today then, and whether it has been undone since.
CREATE TABLE rollover (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(100) NOT NULL REFERENCES sheet(id),
    fy       VARCHAR(20)  NOT NULL,
    made_by  VARCHAR(100) NOT NULL,
    made_on  DATE,
    undone   BOOLEAN      DEFAULT FALSE NOT NULL
);
CREATE INDEX rollover_sheet ON rollover (sheet_id, id);

-- The periods a rollover generated, by name.
CREATE TABLE rollover_period (
    rollover_id BIGINT       NOT NULL REFERENCES rollover(id),
    period_name VARCHAR(200) NOT NULL
);

-- The opening balances a rollover set (rows of period_person_opening).
CREATE TABLE rollover_opening (
    rollover_id BIGINT NOT NULL REFERENCES rollover(id),
    opening_id  BIGINT NOT NULL
);
