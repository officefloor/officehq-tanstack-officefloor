-- A person's tags: trimmed, lower-cased, without repeats, sorted and comma separated (none: no tags).
ALTER TABLE person ADD COLUMN tags VARCHAR(2000);
