-- A person's tags: trimmed, lower-cased, repeats dropped, sorted and comma separated. Null: no tags.
ALTER TABLE person ADD COLUMN tags VARCHAR(2000);
