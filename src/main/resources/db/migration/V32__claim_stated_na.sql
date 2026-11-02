-- A claim a statement bound to n/a: it had no due then because the rate that would apply to it had
-- ended (its date after that rate's to-date). It shows and totals as n/a (a total skips it) until the
-- next statement rebinds or unbinds it, even once a later rate is in force on its date. (A claim
-- bound with no figure for want of any rate keeps a live due, as before.)
ALTER TABLE claim ADD COLUMN stated_na BOOLEAN DEFAULT FALSE NOT NULL;
