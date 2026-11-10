-- A claim's external reference kept in its history too, so the sheet as of a date shows it. History
-- kept before this takes the reference the claim has now.
ALTER TABLE claim_history ADD COLUMN ext_ref VARCHAR(200);
UPDATE claim_history SET ext_ref = (SELECT c.ext_ref FROM claim c WHERE c.id = claim_history.claim_id);
