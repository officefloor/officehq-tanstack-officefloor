-- A sheet's pending SLA in days, not dated (null: none): a PENDING claim whose pending day plus it is
-- before the sheet's today is late pending.
ALTER TABLE sheet ADD COLUMN pending_sla INT;
-- The sheet's today when the claim last went to PENDING (null: it never did).
ALTER TABLE claim ADD COLUMN pending_on DATE;
ALTER TABLE claim_history ADD COLUMN pending_on DATE;
