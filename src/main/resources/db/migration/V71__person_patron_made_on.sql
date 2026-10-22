-- The sheet's today when a patron was named, so a sheet read as of a date shows only the patrons named by then
-- (null: named before this was kept, counted always).
ALTER TABLE person_patron ADD COLUMN made_on DATE;
