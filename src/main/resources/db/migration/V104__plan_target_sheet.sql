-- A plan may name one of its sheet's descendants (target_sheet): its claims are then made for the
-- person of the same name on that sheet, by that sheet's rules. Null makes them on the plan's own sheet.
ALTER TABLE public.plan ADD COLUMN target_sheet VARCHAR(200);
ALTER TABLE asof.plan ADD COLUMN target_sheet VARCHAR(200);
ALTER TABLE asof_hist.plan ADD COLUMN target_sheet VARCHAR(200);
-- The history trigger keeps the column list it was created with: create it again to see the new one.
DROP TRIGGER public.plan_history;
CREATE TRIGGER plan_history AFTER INSERT, UPDATE, DELETE ON public.plan FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';
