-- The sheet the person was transferred from, the last time they were (null: never transferred).
ALTER TABLE person ADD COLUMN origin VARCHAR(100);
