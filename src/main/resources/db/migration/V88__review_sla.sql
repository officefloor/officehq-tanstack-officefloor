-- The sheet's review SLA: a REVIEW claim whose review day (the sheet's today when it went to REVIEW)
-- plus this many days is before the sheet's today is review-late. Null when unset (none). Not dated.
ALTER TABLE public.sheet_setting ADD COLUMN review_sla INT;
ALTER TABLE asof.sheet_setting ADD COLUMN review_sla INT;
ALTER TABLE asof_hist.sheet_setting ADD COLUMN review_sla INT;
-- The history trigger keeps the column list it was created with: create it again to see the new one.
DROP TRIGGER public.sheet_setting_history;
CREATE TRIGGER sheet_setting_history AFTER INSERT, UPDATE, DELETE ON public.sheet_setting FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';
