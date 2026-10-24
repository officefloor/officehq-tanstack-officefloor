-- The sheet's today when the claim last went to REVIEW (null: never in review, or not recorded).
ALTER TABLE claim ADD COLUMN review_on DATE;
