-- A rate saved with a from-date on or after the sheet's today is forward-only: it applies only to claims added after
-- it was saved; the claims already on the sheet keep the rates that applied to them (its earlier versions, or none).
-- Claims and rate versions are stamped from one sequence: a claim's added, a rate's saved. A forward-only rate applies
-- to a claim whose added is greater than its saved. Copies (a cloned sheet) keep their stamps.
CREATE SEQUENCE added_seq;
ALTER TABLE claim ADD COLUMN added BIGINT DEFAULT NEXT VALUE FOR added_seq NOT NULL;
ALTER TABLE rate ADD COLUMN forward BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE rate ADD COLUMN saved BIGINT NOT NULL DEFAULT 0;
ALTER TABLE rate_version ADD COLUMN forward BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE rate_version ADD COLUMN saved BIGINT NOT NULL DEFAULT 0;
