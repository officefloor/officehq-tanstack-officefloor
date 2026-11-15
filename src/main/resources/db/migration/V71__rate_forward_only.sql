-- A rate saved with a from-date on or after the sheet's today is forward-only: it applies only to
-- claims added after it was saved. Its claims_after is the highest claim id there was when it was
-- saved (claims with an id up to it keep the rate that applied to them: the version it replaced, if
-- that applied to them, else none from its from-date). NULL: the rate applies to every claim.
ALTER TABLE rate ADD COLUMN claims_after BIGINT;
ALTER TABLE rate_version ADD COLUMN claims_after BIGINT;
ALTER TABLE rate_history ADD COLUMN claims_after BIGINT;
