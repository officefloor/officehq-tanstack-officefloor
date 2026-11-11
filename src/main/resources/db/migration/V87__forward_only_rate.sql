-- A rate saved with a from-date on or after the sheet's today is forward-only: it applies only to
-- claims added after it was saved. added_after is the highest claim id when it was saved (claims
-- with a greater id were added after it); null for a rate that applies to every claim.
ALTER TABLE rate ADD COLUMN added_after BIGINT;
ALTER TABLE rate_version ADD COLUMN added_after BIGINT;
ALTER TABLE rate_history ADD COLUMN added_after BIGINT;
