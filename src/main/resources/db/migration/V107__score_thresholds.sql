-- The sheet's score thresholds, whole numbers (not dated). Null: blank. While both are set each person shows a band:
-- HIGH at or above score_high, MID at or above score_mid, else LOW.
ALTER TABLE sheet ADD COLUMN score_mid INT;
ALTER TABLE sheet ADD COLUMN score_high INT;
