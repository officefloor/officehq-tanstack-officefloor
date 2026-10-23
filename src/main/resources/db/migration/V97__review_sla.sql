-- The sheet's review SLA (days; null: none; not dated), and a claim's review day: the sheet's today when it went to
-- REVIEW (null: never). A REVIEW claim whose review day plus the SLA is before the sheet's today is review-late.
ALTER TABLE sheet ADD COLUMN review_sla INT;
ALTER TABLE claim ADD COLUMN review_on DATE;
