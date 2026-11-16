-- The kind of each proof added to a claim: receipt (the kind when none was chosen, and of every proof added before
-- kinds were kept) or photo.
ALTER TABLE claim_proof_log ADD COLUMN kind VARCHAR(16) NOT NULL DEFAULT 'receipt';
