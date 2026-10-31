-- A plan runs either every N days (every_days) or monthly on a day of the month (month_day, 1 to
-- 31, a day beyond a month's length falling on its last day): exactly one of the two is set.
ALTER TABLE public.plan ALTER COLUMN every_days SET NULL;
ALTER TABLE asof.plan ALTER COLUMN every_days SET NULL;
ALTER TABLE asof_hist.plan ALTER COLUMN every_days SET NULL;
ALTER TABLE public.plan ADD COLUMN month_day INT;
ALTER TABLE asof.plan ADD COLUMN month_day INT;
ALTER TABLE asof_hist.plan ADD COLUMN month_day INT;
-- The history trigger keeps the column list it was created with: create it again to see the new one.
DROP TRIGGER public.plan_history;
CREATE TRIGGER plan_history AFTER INSERT, UPDATE, DELETE ON public.plan FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';
