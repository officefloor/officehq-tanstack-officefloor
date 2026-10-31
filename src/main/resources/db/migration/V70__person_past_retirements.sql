-- A retired person can be unretired: they are no longer retired (retired_from null) and the
-- retirement is kept as a past one, from its from-date to the sheet's today when unretired. Kept in
-- the order they ended, as `from:to` pairs (yyyy-mm-dd) separated by commas (null: none). No claim
-- dated after the from and before the to of a past retirement can be added or edited for them.
DROP TRIGGER public.person_history;
ALTER TABLE public.person ADD COLUMN past_retirements VARCHAR(4000);
ALTER TABLE asof.person ADD COLUMN past_retirements VARCHAR(4000);
ALTER TABLE asof_hist.person ADD COLUMN past_retirements VARCHAR(4000);
-- The history trigger keeps the column list it was created with: create it again to see the new one.
CREATE TRIGGER person_history AFTER INSERT, UPDATE, DELETE ON public.person FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';
