-- A person transferred onto a sheet merges into one already there with their name or an alias, and their
-- name and aliases join that person's even when one equals an alias of theirs in all but case: aliases are
-- no longer unique on a sheet by the index (the app still refuses a new alias that is taken).
DROP INDEX person_alias_sheet_key;
CREATE INDEX person_alias_sheet_key ON person_alias (sheet_id, alias_key);
