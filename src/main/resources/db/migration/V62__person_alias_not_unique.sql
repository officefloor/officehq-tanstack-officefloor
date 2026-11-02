-- A transfer that merges a person joins their aliases to another's as they are, so an alias may now
-- repeat on the sheet in another case; the checks made when names and aliases are written still apply.
DROP INDEX person_alias_sheet_key;
CREATE INDEX person_alias_sheet_key ON person_alias (sheet, alias_key);
