-- A person's snapshots: one per SNAPSHOT written for them when the sheet's today moved on, the day being left and
-- their due-total as it stood on it (n/a when there was none).
CREATE TABLE person_snapshot (
    person_id BIGINT NOT NULL REFERENCES person (id),
    taken_on DATE NOT NULL,
    due_total VARCHAR(30) NOT NULL,
    seq INT NOT NULL
);
CREATE INDEX person_snapshot_person ON person_snapshot (person_id);
