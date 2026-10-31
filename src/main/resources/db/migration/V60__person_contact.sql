-- A person's contact, kept without the spaces around it and lower-cased; unique on its sheet.
-- Null: none.
ALTER TABLE public.person ADD COLUMN contact VARCHAR(500);
ALTER TABLE asof.person ADD COLUMN contact VARCHAR(500);
ALTER TABLE asof_hist.person ADD COLUMN contact VARCHAR(500);
CREATE UNIQUE INDEX person_sheet_contact ON public.person (sheet_id, contact);
-- The history trigger keeps the column list it was created with: create it again to see the new one.
DROP TRIGGER public.person_history;
CREATE TRIGGER person_history AFTER INSERT, UPDATE, DELETE ON public.person FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';
