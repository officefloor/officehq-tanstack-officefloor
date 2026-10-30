-- People merged into another, as they stood when merged (merged_on: the sheet's today then), so the as-of view of an
-- earlier date still shows them.
CREATE TABLE person_merged AS SELECT * FROM person WITH NO DATA;
ALTER TABLE person_merged ADD COLUMN merged_on DATE;
