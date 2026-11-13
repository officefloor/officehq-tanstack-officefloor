-- A person can be put on hold: while held, their open claims count in no live total (settled ones still do).
ALTER TABLE person ADD COLUMN held BOOLEAN NOT NULL DEFAULT FALSE;
