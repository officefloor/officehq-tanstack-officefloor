-- Whether a person is on hold: while held, their OPEN claims count in no total.
ALTER TABLE person ADD COLUMN held BOOLEAN NOT NULL DEFAULT FALSE;
