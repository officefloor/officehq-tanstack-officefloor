-- The sheet a transferred person came from (null for anyone not transferred in).
ALTER TABLE person ADD COLUMN origin VARCHAR(200);
ALTER TABLE person_merged ADD COLUMN origin VARCHAR(200) BEFORE merged_on;
