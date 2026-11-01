-- The sheet's score thresholds: while both are set, each person shows a band by their score (HIGH at or
-- above the high one, MID at or above the mid one, else LOW). Null when unset. Whole numbers. Not dated.
ALTER TABLE public.sheet_setting ADD COLUMN score_mid INT;
ALTER TABLE public.sheet_setting ADD COLUMN score_high INT;
ALTER TABLE asof.sheet_setting ADD COLUMN score_mid INT;
ALTER TABLE asof.sheet_setting ADD COLUMN score_high INT;
ALTER TABLE asof_hist.sheet_setting ADD COLUMN score_mid INT;
ALTER TABLE asof_hist.sheet_setting ADD COLUMN score_high INT;
-- The history trigger keeps the column list it was created with: create it again to see the new ones.
DROP TRIGGER public.sheet_setting_history;
CREATE TRIGGER sheet_setting_history AFTER INSERT, UPDATE, DELETE ON public.sheet_setting FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';
