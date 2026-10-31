-- A person merged in by a transfer brings their name and aliases as aliases, even one that equals,
-- as names are compared, an alias already there: aliases are no longer unique on their sheet (the
-- screens still refuse a new name or alias that is taken).
DROP INDEX person_alias_sheet_key;
CREATE INDEX person_alias_sheet_key ON person_alias (sheet_id, alias_key);
