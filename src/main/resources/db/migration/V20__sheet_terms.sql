-- The sheet's terms (a number of days): a claim's due-by is its date plus the terms in force.
ALTER TABLE sheet ADD COLUMN terms_days INT NOT NULL DEFAULT 14;
