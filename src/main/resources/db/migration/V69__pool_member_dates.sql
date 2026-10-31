-- A pool membership may carry a from-date and a to-date (both optional; the to-date not before the
-- from-date). A member whose dates do not include the sheet's today is left out of the pool.
DROP TRIGGER public.pool_member_history;
ALTER TABLE public.pool_member ADD COLUMN from_date DATE;
ALTER TABLE public.pool_member ADD COLUMN to_date DATE;
ALTER TABLE asof.pool_member ADD COLUMN from_date DATE;
ALTER TABLE asof.pool_member ADD COLUMN to_date DATE;
ALTER TABLE asof_hist.pool_member ADD COLUMN from_date DATE;
ALTER TABLE asof_hist.pool_member ADD COLUMN to_date DATE;
-- The history trigger keeps the column list it was created with: create it again to see the new ones.
CREATE TRIGGER pool_member_history AFTER INSERT, UPDATE, DELETE ON public.pool_member FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';
