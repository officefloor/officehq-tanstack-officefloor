-- A sheet's review SLA in days, not dated (null: none): a REVIEW claim whose review day plus it is
-- before the sheet's today is late for review.
ALTER TABLE sheet ADD COLUMN review_sla INT;
-- The sheet's today when the claim went to REVIEW (null: it never did).
ALTER TABLE claim ADD COLUMN review_on DATE;
ALTER TABLE claim_history ADD COLUMN review_on DATE;
