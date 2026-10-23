-- A rate is kept as versions: setting a rate for a grade and from-date that already has one keeps the one it
-- replaces as an older version and adds the next. The rate in force for a grade and from-date is its newest version.
ALTER TABLE rate ADD COLUMN version INT DEFAULT 1 NOT NULL;
-- The version each set made (NULL: before versions were kept, counting as 1).
ALTER TABLE rate_log ADD COLUMN version INT;
