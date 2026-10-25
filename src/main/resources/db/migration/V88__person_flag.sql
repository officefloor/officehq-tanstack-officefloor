-- A steward's flag on the person: its reason and the date it is in force until, inclusive (null: not flagged).
ALTER TABLE person ADD COLUMN flag_reason VARCHAR(255);
ALTER TABLE person ADD COLUMN flag_until DATE;
