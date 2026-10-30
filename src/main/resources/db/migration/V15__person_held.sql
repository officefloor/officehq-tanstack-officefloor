-- Whether the person is on hold: while held, their OPEN claims count in no live total (their own
-- claimed and due-total, the sheet's totals, a period's total); their SETTLED claims still count.
ALTER TABLE person ADD COLUMN held BOOLEAN NOT NULL DEFAULT FALSE;
