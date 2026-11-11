-- The sheet's score thresholds, whole numbers, not dated (null: none): while both are set, each person
-- shows a band, HIGH at or above the high one, MID at or above the mid one, else LOW.
ALTER TABLE sheet ADD COLUMN score_mid INT;
ALTER TABLE sheet ADD COLUMN score_high INT;
