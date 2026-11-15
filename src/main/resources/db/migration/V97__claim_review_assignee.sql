-- The user a steward assigned a REVIEW claim to (none: never assigned); kept once it is reviewed.
ALTER TABLE claim ADD COLUMN review_assignee VARCHAR(255);
