-- Every pool member's cut as it stood when the period was closed or soft-closed: one "<pool>\t<member key>\t<cut>"
-- line per member (the member key "p<person id>" or "m<member pool>"). Null on a reopen.
ALTER TABLE period_history ADD COLUMN pool_cuts VARCHAR(1000000);
