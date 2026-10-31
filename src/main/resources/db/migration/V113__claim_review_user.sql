-- The user a steward assigned a REVIEW claim to; NULL when never assigned. Kept when the claim leaves REVIEW.
ALTER TABLE claim ADD COLUMN review_user VARCHAR(200);
ALTER TABLE claim_transferred ADD COLUMN review_user VARCHAR(200) BEFORE transferred_on;
