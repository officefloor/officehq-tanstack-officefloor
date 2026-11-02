-- Whether reminding the claim has escalated it: a claim is escalated at most once ever.
ALTER TABLE claim ADD COLUMN escalated BOOLEAN NOT NULL DEFAULT FALSE;
