-- claim.escalated (V87) was never added to claim_transferred, so a transfer's SELECT c.* copy of a claim failed.
ALTER TABLE claim_transferred ADD COLUMN escalated BOOLEAN DEFAULT FALSE NOT NULL BEFORE transferred_on;
