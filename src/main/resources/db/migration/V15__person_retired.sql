-- A person can be retired from a date. Set at all, they count as retired at once, whatever the
-- date: no claim dated after it can be added or edited for them, and the list hides them unless
-- retired people are shown. Their claims keep counting in every total.
ALTER TABLE person ADD COLUMN retired_from DATE;
