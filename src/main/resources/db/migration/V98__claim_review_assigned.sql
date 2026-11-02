-- The user a steward assigned a REVIEW claim to review (null when never assigned): kept once the review is done.
ALTER TABLE claim ADD COLUMN review_assigned VARCHAR(200);
