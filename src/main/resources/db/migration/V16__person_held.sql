-- A person may be put on hold, and released again. While held, their OPEN claims count in no live total
-- (their claimed and due-total, the sheet's totals, a period's due-total); their SETTLED claims still do.
ALTER TABLE person ADD COLUMN held BOOLEAN NOT NULL DEFAULT FALSE;
