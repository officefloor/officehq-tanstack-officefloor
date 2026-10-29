-- A transfer merged into a person on the other sheet joins their aliases (as a merge does), so an alias may come twice
-- on a sheet in differing case (Addie and addie): alias_key is no longer unique, only indexed. The app still refuses
-- an alias that is taken when one is added.
ALTER TABLE person_alias DROP CONSTRAINT person_alias_sheet_key;
CREATE INDEX person_alias_sheet_key ON person_alias (sheet_id, alias_key);
