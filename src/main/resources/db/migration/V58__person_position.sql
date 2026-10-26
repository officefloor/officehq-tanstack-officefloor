-- A person's place in the sheet's list: the list, the rollup lines and every action that works in list order go by
-- it (lowest first). New people go to the end; existing people keep the order they were added in.
ALTER TABLE person ADD COLUMN list_position BIGINT;
UPDATE person SET list_position = id;
ALTER TABLE person ALTER COLUMN list_position SET NOT NULL;
-- The sheet's snapshots, and their layout as of a date, carry it too.
ALTER TABLE hist.person ADD COLUMN list_position BIGINT;
UPDATE hist.person SET list_position = id;
ALTER TABLE asof.person ADD COLUMN list_position BIGINT;
