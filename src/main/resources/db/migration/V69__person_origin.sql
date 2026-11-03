-- The sheet a transferred person came from (null for anyone added on this sheet).
ALTER TABLE person ADD COLUMN origin_sheet VARCHAR(255);
