-- The sheet's score thresholds, whole numbers; null: blank. While both are set each person shows a band. Not dated.
ALTER TABLE sheet ADD COLUMN score_mid INT;
ALTER TABLE sheet ADD COLUMN score_high INT;
