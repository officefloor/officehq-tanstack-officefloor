-- A sheet's pending SLA in days (null means none; not dated): a PENDING claim whose pending day plus it is before the
-- sheet's today is pending-late. A claim's pending day is the sheet's today when it last went to PENDING.
ALTER TABLE sheet ADD COLUMN pending_sla INT;
ALTER TABLE claim ADD COLUMN pending_day DATE;
