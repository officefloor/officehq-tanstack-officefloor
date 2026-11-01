-- A person's own terms can be dated: each is in force from its from-date, kept as `from:terms` pairs
-- (yyyy-mm-dd, terms blank for none) sorted by from-date and separated by commas (null: none).
-- `person.terms` stays the terms from the beginning of time. The terms in force on a date are the
-- ones with the latest from-date on or before it, else `person.terms`.
ALTER TABLE public.person ADD COLUMN dated_terms VARCHAR(4000);
ALTER TABLE asof.person ADD COLUMN dated_terms VARCHAR(4000);
ALTER TABLE asof_hist.person ADD COLUMN dated_terms VARCHAR(4000);
-- The history trigger keeps the column list it was created with: create it again to see the new one.
DROP TRIGGER public.person_history;
CREATE TRIGGER person_history AFTER INSERT, UPDATE, DELETE ON public.person FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';
