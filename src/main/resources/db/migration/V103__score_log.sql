-- The sheet's score log switch: while on, a change of the sheet's today writes a SCORE line per person
-- (their score on the day being left) before the SNAPSHOT lines. False when blank. Not dated.
ALTER TABLE public.sheet_setting ADD COLUMN score_log BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE asof.sheet_setting ADD COLUMN score_log BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE asof_hist.sheet_setting ADD COLUMN score_log BOOLEAN DEFAULT FALSE NOT NULL;
-- The history trigger keeps the column list it was created with: create it again to see the new one.
DROP TRIGGER public.sheet_setting_history;
CREATE TRIGGER sheet_setting_history AFTER INSERT, UPDATE, DELETE ON public.sheet_setting FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';
