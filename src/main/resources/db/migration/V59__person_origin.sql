-- The sheet a person was last transferred from (NULL: they were never transferred).
ALTER TABLE person ADD COLUMN origin VARCHAR(255);
