-- Forward-only rates. Each claim takes the next number of claim_added_seq when it is added (by any path), so
-- claims added later have higher numbers (claims already on a sheet take 0). A rate saved with a from-date on or
-- after the sheet's today is forward-only: it takes the next number too (after_seq) and applies only to claims
-- with a higher one, those added after it was saved. NULL after_seq: the rate applies to every claim. Kept on each
-- version of a rate (rate, rate_version) and on each rate set (rate_history).
CREATE SEQUENCE claim_added_seq START WITH 1;
ALTER TABLE claim ADD COLUMN added_seq BIGINT NOT NULL DEFAULT 0;
ALTER TABLE claim ALTER COLUMN added_seq SET DEFAULT NEXT VALUE FOR claim_added_seq;
ALTER TABLE rate ADD COLUMN after_seq BIGINT;
ALTER TABLE rate_version ADD COLUMN after_seq BIGINT;
ALTER TABLE rate_history ADD COLUMN after_seq BIGINT;
