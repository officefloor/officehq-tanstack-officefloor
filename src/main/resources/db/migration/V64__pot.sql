-- The sheet's pot: shared out among the members of each balanced pool by their shares. Null when
-- unset (none). Not dated.
ALTER TABLE public.sheet_setting ADD COLUMN pot DECIMAL(12,2);
ALTER TABLE asof.sheet_setting ADD COLUMN pot DECIMAL(12,2);
ALTER TABLE asof_hist.sheet_setting ADD COLUMN pot DECIMAL(12,2);
-- The history trigger keeps the column list it was created with: create it again to see the new one.
DROP TRIGGER public.sheet_setting_history;
CREATE TRIGGER sheet_setting_history AFTER INSERT, UPDATE, DELETE ON public.sheet_setting FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';
