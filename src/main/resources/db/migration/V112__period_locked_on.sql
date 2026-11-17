-- The sheet's today a period was last locked on (null: not locked, or locked before this was kept), so the sheet as of
-- an earlier date shows it unlocked.
ALTER TABLE period ADD COLUMN locked_on DATE;
