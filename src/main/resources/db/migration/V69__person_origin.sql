-- The sheet a person was transferred from, set as they arrive; null: they were not transferred here.
ALTER TABLE person ADD COLUMN origin VARCHAR(200);
