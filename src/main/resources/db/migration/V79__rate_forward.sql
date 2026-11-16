-- A rate saved with a from-date on or after the sheet's today is forward-only: it applies only to claims added after
-- it was saved. Claims are ordered as added by added_seq, given from one sequence for every sheet (a copied claim keeps
-- its original's, so a copied sheet keeps which rates apply to which claims). A forward-only rate keeps in after_seq the
-- highest added_seq of any claim when it was saved: it applies to a claim only when the claim's added_seq is above it.
-- after_seq is null for a rate that applies to every claim. Claims added before this predate every forward-only rate.
CREATE SEQUENCE claim_added_seq START WITH 1;
ALTER TABLE claim ADD COLUMN added_seq BIGINT DEFAULT NEXT VALUE FOR claim_added_seq NOT NULL;

ALTER TABLE rate ADD COLUMN after_seq BIGINT;
ALTER TABLE rate_version ADD COLUMN after_seq BIGINT;
ALTER TABLE rate_log ADD COLUMN after_seq BIGINT;
