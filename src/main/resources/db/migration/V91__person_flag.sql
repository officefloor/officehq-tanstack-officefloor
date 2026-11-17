-- The reason and until-date a steward last flagged the person with (null: never flagged). While the sheet's today is on
-- or before the until-date only a steward may open a claim of theirs.
ALTER TABLE person ADD COLUMN flag_reason VARCHAR(1000);
ALTER TABLE person ADD COLUMN flag_until DATE;
