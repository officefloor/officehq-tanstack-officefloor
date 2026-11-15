-- A steward may flag a person with a reason until a date (both null: never flagged); a new flag
-- replaces the one before. While the sheet's today is on or before the until-date only a steward
-- may open their claims.
ALTER TABLE person ADD COLUMN flag_reason VARCHAR(500);
ALTER TABLE person ADD COLUMN flag_until DATE;
