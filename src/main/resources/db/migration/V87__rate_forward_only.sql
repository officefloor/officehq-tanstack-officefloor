-- A rate saved with a from-date on or after the sheet's today is forward-only: it applies only to claims added after
-- it was saved. after_claim is the highest claim id when it was saved (claims with a higher id are added after it);
-- NULL: it applies to every claim. A claim not reached by a version of a grade's rate from a from-date uses the newest
-- version that does reach it.
ALTER TABLE rate ADD COLUMN after_claim BIGINT;
ALTER TABLE rate_log ADD COLUMN after_claim BIGINT;
