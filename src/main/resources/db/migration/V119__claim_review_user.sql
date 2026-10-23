-- The user a steward assigned a REVIEW claim to review (null: none). Kept once the claim leaves REVIEW; only its
-- REVIEW claims count toward the user's reviews.
ALTER TABLE claim ADD COLUMN review_user VARCHAR(100);
