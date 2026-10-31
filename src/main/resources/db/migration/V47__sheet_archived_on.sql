-- The sheet's today when it was archived, so a sheet seen as of an earlier date is not archived
-- (see sheet.AsOf). Null for a sheet archived before this was kept: archived as of any date.
ALTER TABLE public.sheet ADD COLUMN archived_on DATE;
ALTER TABLE asof.sheet ADD COLUMN archived_on DATE;
