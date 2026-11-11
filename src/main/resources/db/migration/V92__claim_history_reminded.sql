-- A claim's reminded date kept in its history too, so the sheet as of a date shows it. History kept
-- before this takes the reminded date the claim has now, where it is on or before that history's day.
ALTER TABLE claim_history ADD COLUMN reminded_on DATE;
UPDATE claim_history SET reminded_on = (SELECT c.reminded_on FROM claim c WHERE c.id = claim_history.claim_id
    AND c.reminded_on <= claim_history.set_on);
