-- The sheet a person was transferred from (null for anyone not transferred): shown on their row, and the audit
-- viewer follows it to list that sheet's lines about them after this sheet's.
ALTER TABLE person ADD COLUMN origin VARCHAR(200);
