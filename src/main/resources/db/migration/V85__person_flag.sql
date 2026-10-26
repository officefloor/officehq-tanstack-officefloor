-- A steward's flag on a person: its reason and the date it holds until (inclusive). While the sheet's today is on or
-- before that date the person's row shows the reason and only a steward may open their claims; null: never flagged.
ALTER TABLE person ADD COLUMN flag_reason VARCHAR(255);
ALTER TABLE person ADD COLUMN flag_until DATE;
ALTER TABLE hist.person ADD COLUMN flag_reason VARCHAR(255);
ALTER TABLE hist.person ADD COLUMN flag_until DATE;
ALTER TABLE asof.person ADD COLUMN flag_reason VARCHAR(255);
ALTER TABLE asof.person ADD COLUMN flag_until DATE;
