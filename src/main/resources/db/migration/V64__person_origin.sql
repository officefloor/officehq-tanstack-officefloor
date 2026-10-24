-- The sheet a transferred person came from (null: they were not transferred here).
ALTER TABLE person ADD COLUMN origin_sheet VARCHAR(255);
