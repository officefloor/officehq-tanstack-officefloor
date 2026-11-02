-- A sheet's terms: the number of days after a claim's date that it is due by. It starts at 14.
ALTER TABLE sheet ADD COLUMN terms INT NOT NULL DEFAULT 14;
