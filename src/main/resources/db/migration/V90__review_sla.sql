-- A sheet's review SLA in days (null means none; not dated): a REVIEW claim whose review day plus it is before the
-- sheet's today is review-late. A claim's review day is the sheet's today when it last went to REVIEW.
ALTER TABLE sheet ADD COLUMN review_sla INT;
ALTER TABLE claim ADD COLUMN review_day DATE;
