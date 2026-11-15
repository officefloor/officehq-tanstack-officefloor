-- A person may have an approver: a user name (null: none) who may approve or reject the person's
-- PENDING claims as a steward may (though not one they opened or last edited).
ALTER TABLE person ADD COLUMN approver VARCHAR(200);
