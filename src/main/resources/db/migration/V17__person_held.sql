-- A person on hold: while held, their OPEN claims count in no live total; their SETTLED ones still do.
ALTER TABLE person ADD COLUMN held BOOLEAN DEFAULT FALSE NOT NULL;
