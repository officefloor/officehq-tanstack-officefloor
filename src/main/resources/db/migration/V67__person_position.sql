-- A person's place in their sheet's list: the list (and every action that works in list order)
-- runs by ascending position. A new person goes to the end; moving a person up or down swaps their
-- position with their neighbour's. People already on a sheet keep the order they were added in.
DROP TRIGGER public.person_history;
ALTER TABLE public.person ADD COLUMN position BIGINT;
ALTER TABLE asof.person ADD COLUMN position BIGINT;
ALTER TABLE asof_hist.person ADD COLUMN position BIGINT;
UPDATE public.person SET position = id;
UPDATE asof_hist.person SET position = id;
CREATE INDEX person_sheet_position ON public.person (sheet_id, position);
-- The history trigger keeps the column list it was created with: create it again to see the new one.
CREATE TRIGGER person_history AFTER INSERT, UPDATE, DELETE ON public.person FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';
