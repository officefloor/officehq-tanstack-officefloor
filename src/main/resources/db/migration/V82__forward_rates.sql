-- Forward-only rates. A rate saved with a from-date on or after the sheet's today applies only to claims added after it
-- was saved; one with no from-date, or a from-date before the sheet's today, applies to every claim. Both a rate save and
-- a claim's arrival on the sheet draw a mark from one sequence, so a forward-only rate (after_mark: its mark; 0 for a rate
-- applying to every claim) applies to a claim exactly when the claim's rate_mark is greater. A claim from before this
-- was kept has mark 0: every forward-only rate is newer than it.
CREATE SEQUENCE rate_mark_seq START WITH 1;
ALTER TABLE rate ADD COLUMN after_mark BIGINT NOT NULL DEFAULT 0;
ALTER TABLE rate_version ADD COLUMN after_mark BIGINT NOT NULL DEFAULT 0;
ALTER TABLE rate_history ADD COLUMN after_mark BIGINT NOT NULL DEFAULT 0;
ALTER TABLE claim ADD COLUMN rate_mark BIGINT NOT NULL DEFAULT 0;
ALTER TABLE claim_transferred ADD COLUMN rate_mark BIGINT NOT NULL DEFAULT 0 BEFORE transferred_on;
ALTER TABLE claim_history ADD COLUMN rate_mark BIGINT NOT NULL DEFAULT 0;
