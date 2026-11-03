-- A person's snapshots: one per SNAPSHOT line written for them, each with the day being left (the sheet's
-- today before the change of today) and their due-total as it stood (two decimals, or 'n/a'), oldest first (by id).
CREATE TABLE person_snapshot (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200) NOT NULL REFERENCES sheet (id),
    person_id BIGINT       NOT NULL REFERENCES person (id),
    left_day  DATE,
    due_total VARCHAR(20)  NOT NULL
);
CREATE INDEX person_snapshot_person ON person_snapshot (person_id, id);
