-- Closing or soft-closing a period also fixes every pool member's cut for it as it stood at that moment (the per-period
-- cut on the pools screen), one line per member as "<member key>\t<cut>" (an empty cut: none), joined by newlines; null
-- on a reopen, so the cut is live again, and on a close made before this was kept.
ALTER TABLE period_log ADD COLUMN pool_cuts CLOB;
