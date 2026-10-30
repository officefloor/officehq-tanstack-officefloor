-- A person's own terms in days; null for none (their grade's terms, else the sheet's, apply).
ALTER TABLE person ADD COLUMN terms INT;
