-- The sheet's maximum of open claims per person: opening a claim is refused while its person already
-- has that many OPEN or PENDING claims. Null for none. A whole number. Not dated.
ALTER TABLE public.sheet_setting ADD COLUMN max_open INT;
ALTER TABLE asof.sheet_setting ADD COLUMN max_open INT;
ALTER TABLE asof_hist.sheet_setting ADD COLUMN max_open INT;
-- The history trigger keeps the column list it was created with: create it again to see the new one.
DROP TRIGGER public.sheet_setting_history;
CREATE TRIGGER sheet_setting_history AFTER INSERT, UPDATE, DELETE ON public.sheet_setting FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';
