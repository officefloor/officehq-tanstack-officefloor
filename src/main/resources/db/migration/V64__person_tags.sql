-- A person's tags, lower-cased, sorted and comma separated (empty for none): a claim counts in a tag's due-total when
-- it or its person carries the tag.
ALTER TABLE person ADD COLUMN tags VARCHAR(4000) DEFAULT '' NOT NULL;
