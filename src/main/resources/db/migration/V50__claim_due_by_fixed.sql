-- A claim's due-by is fixed again when it is added (counted over the sheet's holidays as they stand then), and is
-- re-fixed only when a holiday is added on or before it while the claim is neither SETTLED nor VOID (see
-- ClaimRepository.refix). Existing claims (null) are worked out when read, as before, until next fixed.
ALTER TABLE claim ADD COLUMN due_by DATE;
ALTER TABLE hist.claim ADD COLUMN due_by DATE;
ALTER TABLE asof.claim ADD COLUMN due_by DATE;
