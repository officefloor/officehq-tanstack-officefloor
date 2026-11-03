-- The user a steward assigned a REVIEW claim to (null when never assigned). Kept after the claim leaves REVIEW.
ALTER TABLE claim ADD COLUMN review_assigned VARCHAR(255);
