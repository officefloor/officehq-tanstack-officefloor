-- A person's place in the sheet's list: people are listed by it (ascending), not by id. Those
-- already on a sheet keep the order they were added in; a new person goes to the end.
ALTER TABLE person ADD COLUMN list_pos BIGINT;
UPDATE person SET list_pos = id;
ALTER TABLE person ALTER COLUMN list_pos SET NOT NULL;
CREATE INDEX person_sheet_pos ON person(sheet_id, list_pos);
