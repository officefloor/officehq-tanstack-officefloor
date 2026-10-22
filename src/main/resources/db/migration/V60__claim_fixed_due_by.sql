-- A claim's due-by is fixed again: worked out when the claim is made (the terms_days-th business day after due_from,
-- over the sheet's holidays then) and stored. Adding a holiday re-fixes it, over the holidays then on the sheet, for each
-- claim not SETTLED or VOID whose due-by is on or after the new holiday; the rest keep theirs. A claim stored before this
-- has none (null) and is worked out over the sheet's holidays as they are now, until a holiday is next added, which
-- first fixes it.
ALTER TABLE claim ADD COLUMN due_by DATE;
ALTER TABLE claim_history ADD COLUMN due_by DATE;
