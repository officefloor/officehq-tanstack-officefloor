-- The sheet's pending SLA (days; null: none; not dated), and a claim's pending day: the sheet's today when it last went
-- to PENDING (null: never). A PENDING claim whose pending day plus the SLA is before the sheet's today is pending-late.
ALTER TABLE sheet ADD COLUMN pending_sla INT;
ALTER TABLE claim ADD COLUMN pending_on DATE;
