-- The sheet a transferred person came from, and the name they had there (both null: the person
-- was not transferred here). The audit viewer follows a person's lines back to that sheet.
DROP TRIGGER public.person_history;
ALTER TABLE public.person ADD COLUMN origin_sheet VARCHAR(255);
ALTER TABLE public.person ADD COLUMN origin_name VARCHAR(255);
ALTER TABLE asof.person ADD COLUMN origin_sheet VARCHAR(255);
ALTER TABLE asof.person ADD COLUMN origin_name VARCHAR(255);
ALTER TABLE asof_hist.person ADD COLUMN origin_sheet VARCHAR(255);
ALTER TABLE asof_hist.person ADD COLUMN origin_name VARCHAR(255);
-- The history trigger keeps the column list it was created with: create it again to see the new ones.
CREATE TRIGGER person_history AFTER INSERT, UPDATE, DELETE ON public.person FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';
