-- The sheet's pending SLA in days (not dated). Null: none.
ALTER TABLE sheet ADD COLUMN pending_sla INT;
