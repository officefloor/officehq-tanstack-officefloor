-- The user a REVIEW claim is assigned to for review (null: none). Kept when the claim leaves REVIEW.
ALTER TABLE claim ADD COLUMN review_assignee VARCHAR(255);
