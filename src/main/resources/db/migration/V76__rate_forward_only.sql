-- A rate saved with a from-date on or after the sheet's today is forward-only: it applies only to claims added after it
-- was saved. Its log line keeps the highest claim id there was when it was saved (null: it applies to every claim), so
-- a claim with an id above it takes it, and one at or below it keeps the rates that applied to it before.
ALTER TABLE rate_log ADD COLUMN forward_after BIGINT;
