-- A person transferred into a sheet that has someone of the same name or alias merges into them, their name and
-- aliases joining the other's even where one already equals another there (ignoring case and surrounding spaces). So
-- an alias is no longer unique on its sheet by index: the app still refuses an alias added that is taken there.
DROP INDEX person_alias_sheet_key;
CREATE INDEX person_alias_sheet_key ON person_alias (sheet_id, alias_key);
