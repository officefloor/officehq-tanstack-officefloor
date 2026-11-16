-- The screen can show a sheet as it stood at the end of a date (an as-of view): every change made while the sheet's
-- today was on or before it counts, every later one does not. These keep the sheet's today when each change was made
-- (null: made before this was kept; there from the beginning).

-- The sheet's today when it was archived.
ALTER TABLE sheet_archived ADD COLUMN archived_on DATE;

-- The sheet's today when the period was added.
ALTER TABLE period ADD COLUMN added_on DATE;

-- Every close, soft close and reopen of a period, in the order made, with the figures it fixed: total is the
-- period's due-total, person_totals each person's as "personId=total" joined by ";" (both null on a reopen).
CREATE TABLE period_log (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id      VARCHAR(200)  NOT NULL,
    period_id     BIGINT        NOT NULL,
    closed        BOOLEAN       NOT NULL,
    soft_closed   BOOLEAN       NOT NULL,
    closed_total  DECIMAL(12, 2),
    person_totals VARCHAR(8000),
    made_on       DATE          NOT NULL
);
CREATE INDEX period_log_sheet ON period_log (sheet_id, period_id, id);

-- The sheet's today when a patron was named, and when a user started watching a person.
ALTER TABLE patron ADD COLUMN added_on DATE;
ALTER TABLE watch ADD COLUMN added_on DATE;
