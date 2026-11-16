-- The sheet's today when an alias was given (null: before it was kept), so a sheet read as of a date leaves out later ones.
ALTER TABLE person_alias ADD COLUMN added_on DATE;
