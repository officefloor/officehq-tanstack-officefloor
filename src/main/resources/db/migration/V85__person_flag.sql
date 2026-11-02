-- A person's flag, set by a steward: its reason and its until-date (both null when never flagged). While the sheet's
-- today is on or before the until-date only a steward may open a claim of theirs; after it the flag has no effect.
ALTER TABLE person ADD COLUMN flag_reason VARCHAR(255);
ALTER TABLE person ADD COLUMN flag_until DATE;
