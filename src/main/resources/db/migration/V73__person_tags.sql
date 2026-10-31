-- A person's tags, lower-cased, sorted and comma separated ("a,b"), as a claim's are; null when
-- they have none. A claim counts in a tag's due-total when it or its person carries the tag.
ALTER TABLE public.person ADD COLUMN tags VARCHAR(2000);
ALTER TABLE asof.person ADD COLUMN tags VARCHAR(2000);
ALTER TABLE asof_hist.person ADD COLUMN tags VARCHAR(2000);
-- The history trigger keeps the column list it was created with: create it again to see the new one.
DROP TRIGGER public.person_history;
CREATE TRIGGER person_history AFTER INSERT, UPDATE, DELETE ON public.person FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';
