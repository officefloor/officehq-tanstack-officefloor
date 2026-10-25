-- The user name a steward assigned the claim's review to (null: never assigned).
ALTER TABLE claim ADD COLUMN review_assigned VARCHAR(255);
