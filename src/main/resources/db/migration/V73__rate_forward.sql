-- A claim carries when it was added (claim.added, from claim_added_seq, rising as claims are added by any path; a
-- copy of a claim keeps it). A rate saved with a from-date on or after the sheet's today is forward-only: forward_after
-- is the sequence's value when it was saved, and it applies only to claims added after (added greater than it); a
-- claim added before uses the version it replaced (none when it replaced none). A null forward_after applies to every
-- claim. Older versions keep theirs in rate_version.
CREATE SEQUENCE claim_added_seq;
ALTER TABLE claim ADD COLUMN added BIGINT DEFAULT NEXT VALUE FOR claim_added_seq;
ALTER TABLE rate ADD COLUMN forward_after BIGINT;
ALTER TABLE rate_version ADD COLUMN forward_after BIGINT;
