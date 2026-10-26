-- A person's snapshot lines: one per SNAPSHOT written for them as the sheet's today moved on, with the day being left
-- and their due-total as it stood then (null: n/a). Listed oldest first (by id).
CREATE TABLE person_snapshot (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200)   NOT NULL REFERENCES sheet (id),
    person_id BIGINT         NOT NULL REFERENCES person (id),
    left_on   DATE           NOT NULL,
    due_total DECIMAL(19, 2)
);
CREATE INDEX person_snapshot_person ON person_snapshot (person_id, id);

-- Snapshotted with the rest of the screen (see V40, V41).
CREATE TABLE hist.person_snapshot AS SELECT t.*, CAST(NULL AS BIGINT) AS snapshot_id FROM person_snapshot t WHERE FALSE;
CREATE INDEX hist_person_snapshot_snapshot ON hist.person_snapshot (snapshot_id);
CREATE TABLE asof.person_snapshot AS SELECT * FROM person_snapshot WHERE FALSE;
