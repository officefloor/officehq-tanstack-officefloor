-- A rate withdrawn: a steward takes back the newest version of a grade's rate from a from-date, and the version
-- before it (when there is one) applies again. Kept in rate_history as a row marked withdrawn (its pct the
-- withdrawn version's): it takes back the last rate set from that from-date.
ALTER TABLE rate_history ADD COLUMN withdrawn BOOLEAN NOT NULL DEFAULT FALSE;
