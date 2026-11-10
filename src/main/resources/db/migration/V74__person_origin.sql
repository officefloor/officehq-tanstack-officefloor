-- The sheet a person was transferred from (their latest transfer); null for anyone never transferred.
ALTER TABLE person ADD COLUMN origin_sheet VARCHAR(200);
