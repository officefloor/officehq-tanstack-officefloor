-- A person transferred onto a sheet and merged there into someone sharing their name keeps their name and aliases as
-- that person's aliases, even where one equals, ignoring case, an alias already there: aliases are no longer unique.
DROP INDEX person_alias_sheet_key;
CREATE INDEX person_alias_sheet_key ON person_alias (sheet_id, alias_key);
