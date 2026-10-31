-- The person's approver: a user name (null for none) who may approve or reject the person's PENDING claims as a
-- steward may.
ALTER TABLE person ADD COLUMN approver VARCHAR(255);
ALTER TABLE person_merged ADD COLUMN approver VARCHAR(255) BEFORE merged_on;
