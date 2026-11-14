-- The sheet's pending SLA in days: a PENDING claim whose pending day (the sheet's today when it last went to PENDING)
-- plus this many days is before the sheet's today is late; null: none. Not dated.
ALTER TABLE sheet ADD COLUMN pending_sla INT;
