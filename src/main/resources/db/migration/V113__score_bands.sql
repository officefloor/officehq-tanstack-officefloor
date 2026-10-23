-- The sheet's score thresholds (whole numbers; null: blank; not dated). While both are set, a person's band is HIGH
-- when their score is at or above the high one, MID when at or above the mid one, else LOW.
ALTER TABLE sheet ADD COLUMN score_mid INT;
ALTER TABLE sheet ADD COLUMN score_high INT;
