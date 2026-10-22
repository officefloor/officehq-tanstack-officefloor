-- A transfer that merges the arriving person into one of the same name or alias joins their aliases and name to
-- that person's, so two aliases on a sheet may now compare equal (the app still refuses a new alias or name in use).
DROP INDEX person_alias_sheet_key;
CREATE INDEX person_alias_sheet_key ON person_alias(sheet_id, alias_key);
