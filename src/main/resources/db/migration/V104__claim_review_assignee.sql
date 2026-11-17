-- A steward may assign a REVIEW claim to a user, who is then shown as its reviewer (kept once the claim leaves REVIEW).
-- Null: never assigned.
ALTER TABLE claim ADD COLUMN review_assignee VARCHAR(200);
