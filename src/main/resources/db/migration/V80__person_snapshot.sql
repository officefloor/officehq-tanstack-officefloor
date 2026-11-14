-- A person's snapshot lines: one per SNAPSHOT written for them as the sheet's today moved on, with the day being left
-- and their due-total on it as shown (two decimals, or n/a).
CREATE TABLE person_snapshot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(100) NOT NULL REFERENCES sheet (id),
    person_id BIGINT NOT NULL REFERENCES person (id),
    left_day DATE NOT NULL,
    due_total VARCHAR(20) NOT NULL
);
CREATE INDEX person_snapshot_sheet ON person_snapshot (sheet_id, person_id, id);
