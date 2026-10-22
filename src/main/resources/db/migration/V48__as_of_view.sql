-- What the screen shows as of a date: the rest of the sheet kept with the sheet's today when each change was made
-- (made_on; NULL: before history was kept, so it counts on every date). See V47 for people, rates, settings and claims.

-- The day the sheet was archived (NULL: not archived, or archived before history was kept).
ALTER TABLE sheet ADD COLUMN archived_on DATE;

-- The day each statement was taken, and whether (and the day) it was cancelled since: a cancelled statement is kept,
-- so the statements a person had at the end of an earlier day can still be told.
ALTER TABLE person_statement ADD COLUMN made_on DATE;
ALTER TABLE person_statement ADD COLUMN cancelled BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE person_statement ADD COLUMN cancelled_on DATE;

-- The day a period was added (NULL: before history was kept).
ALTER TABLE period ADD COLUMN added_on DATE;

-- Each period as it stood after every close, soft-close and reopen, oldest first: whether it was closed or
-- soft-closed then and the due-totals fixed then (the period's and each person's).
CREATE TABLE period_history (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id     VARCHAR(100)   NOT NULL REFERENCES sheet(id),
    period_name  VARCHAR(200)   NOT NULL,
    closed       BOOLEAN        NOT NULL,
    soft         BOOLEAN        NOT NULL,
    closed_total DECIMAL(14, 2),
    made_on      DATE
);
CREATE INDEX period_history_sheet ON period_history(sheet_id, period_name, id);
INSERT INTO period_history (sheet_id, period_name, closed, soft, closed_total, made_on)
    SELECT sheet_id, name, closed, soft, closed_total, NULL FROM period WHERE closed OR soft;

CREATE TABLE period_history_total (
    history_id BIGINT         NOT NULL REFERENCES period_history(id),
    person_id  BIGINT         NOT NULL,
    total      DECIMAL(14, 2) NOT NULL,
    PRIMARY KEY (history_id, person_id)
);
INSERT INTO period_history_total (history_id, person_id, total)
    SELECT h.id, t.person_id, t.total FROM period_history h JOIN period_person_total t
        ON t.sheet_id = h.sheet_id AND t.period_name = h.period_name;
