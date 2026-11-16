-- A transfer that merges the arriving person into someone already on the sheet joins their aliases, so a person may now
-- hold aliases that differ only in case (one of theirs matching one of the arriving person's names): an alias is unique
-- on the sheet as written; the app still keeps a newly given alias apart from every name and alias there, case ignored.
DROP INDEX person_alias_sheet_key;
CREATE UNIQUE INDEX person_alias_sheet_alias ON person_alias (sheet_id, alias);
