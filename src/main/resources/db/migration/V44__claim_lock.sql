-- Who has the claim's editor open, and on which of the sheet's todays: the lock holds only while
-- the sheet's today is that date. NULL: no one.
ALTER TABLE claim ADD COLUMN locked_by VARCHAR(200);
ALTER TABLE claim ADD COLUMN locked_on DATE;
