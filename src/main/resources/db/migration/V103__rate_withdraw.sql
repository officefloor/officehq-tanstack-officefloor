-- A steward can withdraw a rate's newest version: the version before it applies again, or, with none,
-- the rate is gone. A withdrawal that leaves no rate is kept in the history as a withdrawn line, so the
-- sheet shown as of a date on or after it has no rate for that grade and from-date.
ALTER TABLE rate_history ADD COLUMN withdrawn BOOLEAN NOT NULL DEFAULT FALSE;
