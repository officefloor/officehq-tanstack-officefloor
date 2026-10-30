-- A steward's flag on a person: a reason and an until date (null for none). While the sheet's today is on or
-- before the until date the reason is shown and only a steward may open the person's claims.
ALTER TABLE person ADD COLUMN flag_reason VARCHAR(500);
ALTER TABLE person ADD COLUMN flag_until DATE;
ALTER TABLE person_merged ADD COLUMN flag_reason VARCHAR(500) BEFORE merged_on;
ALTER TABLE person_merged ADD COLUMN flag_until DATE BEFORE merged_on;
