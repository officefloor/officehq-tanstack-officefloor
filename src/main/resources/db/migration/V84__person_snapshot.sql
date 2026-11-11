-- A SNAPSHOT written for a person when the sheet's today changed: the day being left and their
-- due-total as it stood on it (n/a when none), oldest first.
CREATE TABLE person_snapshot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet(id),
    person_id BIGINT NOT NULL REFERENCES person(id),
    snapshot_day DATE NOT NULL,
    due_total VARCHAR(40) NOT NULL
);
CREATE INDEX person_snapshot_sheet ON person_snapshot(sheet_id, person_id, id);
