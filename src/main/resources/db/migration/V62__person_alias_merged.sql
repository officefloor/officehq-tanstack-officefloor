-- A transfer that merges the arriving person into one already on the sheet joins their name and
-- aliases to that person's, so the same alias (as names are compared) may now appear twice on a
-- person: an alias is no longer unique on the sheet by the store, only as the screens add it.
DROP INDEX person_alias_sheet_key;
CREATE INDEX person_alias_sheet_key ON person_alias (sheet_id, alias_key);
