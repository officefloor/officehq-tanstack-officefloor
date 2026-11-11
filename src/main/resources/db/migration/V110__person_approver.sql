-- The user name a person's PENDING claims may also be approved or rejected by (null: none).
ALTER TABLE person ADD COLUMN approver VARCHAR(200);
