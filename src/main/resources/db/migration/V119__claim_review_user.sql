-- The user a steward assigned a REVIEW claim to review (null: none). Kept once it leaves REVIEW.
ALTER TABLE claim ADD COLUMN review_user VARCHAR(200);
ALTER TABLE claim_history ADD COLUMN review_user VARCHAR(200);
