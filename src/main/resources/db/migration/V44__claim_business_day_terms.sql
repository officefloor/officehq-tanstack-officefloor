-- A claim's due-by now counts business days: it is the terms-th day after the date it counts from that is not a
-- Saturday, a Sunday or a holiday on the sheet, worked out whenever the claim is read (so a holiday added later moves
-- it). Each claim keeps the date its due-by counts from (its date when it was added: a later change to the date leaves
-- it) and the terms it was fixed with (those in force on that date for its person). Existing claims count from their
-- date, with the days their fixed due-by lay after it as their terms.
ALTER TABLE claim ADD COLUMN due_from DATE;
ALTER TABLE claim ADD COLUMN terms INT;
UPDATE claim SET due_from = claim_date, terms = GREATEST(DATEDIFF(DAY, claim_date, due_by), 0);
ALTER TABLE claim ALTER COLUMN due_from SET NOT NULL;
ALTER TABLE claim ALTER COLUMN terms SET NOT NULL;
ALTER TABLE claim DROP COLUMN due_by;

-- Snapshotted with the rest of the screen (see V40, V41).
ALTER TABLE hist.claim ADD COLUMN due_from DATE;
ALTER TABLE hist.claim ADD COLUMN terms INT;
UPDATE hist.claim SET due_from = claim_date, terms = GREATEST(DATEDIFF(DAY, claim_date, due_by), 0);
ALTER TABLE hist.claim DROP COLUMN due_by;
ALTER TABLE asof.claim ADD COLUMN due_from DATE;
ALTER TABLE asof.claim ADD COLUMN terms INT;
ALTER TABLE asof.claim DROP COLUMN due_by;
