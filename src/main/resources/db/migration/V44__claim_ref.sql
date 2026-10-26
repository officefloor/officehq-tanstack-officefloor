-- A claim may carry an external reference (null: none), unique on its sheet without regard to case: ref_key is the
-- reference in lower case, which the unique index holds to once per sheet (nulls repeat freely). Kept in the claim's
-- history too (see V39). The journal trigger is made again around the change, as in V41.
DROP TRIGGER claim_journal;
ALTER TABLE claim ADD COLUMN ref VARCHAR(4000);
ALTER TABLE claim ADD COLUMN ref_key VARCHAR(4000);
ALTER TABLE claim_history ADD COLUMN ref VARCHAR(4000);
ALTER TABLE claim_history ADD COLUMN ref_key VARCHAR(4000);
CREATE UNIQUE INDEX claim_ref_unique ON claim (sheet_id, ref_key);
CREATE TRIGGER claim_journal AFTER INSERT, UPDATE, DELETE ON claim FOR EACH ROW CALL 'net.officefloor.hq.app.history.Journal';
