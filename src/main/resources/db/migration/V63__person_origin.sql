-- The sheet a person was last transferred from (null: never transferred), shown on their row and used by the audit
-- viewer to follow their history back to that sheet.
ALTER TABLE person ADD COLUMN origin VARCHAR(255);
