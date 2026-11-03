-- A steward's flag on a person: its reason and the date it is in force until (inclusive; null: never flagged).
-- While the sheet's today is on or before that date only a steward may open a claim of the person.
ALTER TABLE person ADD COLUMN flag_reason VARCHAR(200);
ALTER TABLE person ADD COLUMN flag_until DATE;
