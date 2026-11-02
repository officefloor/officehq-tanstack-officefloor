-- A person's snapshot lines: one per SNAPSHOT written for them as the sheet's today moved, with the day being left
-- and their due-total on it (two decimals, or n/a), listed oldest first (by id).
CREATE TABLE person_snapshot (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet     VARCHAR(200) NOT NULL,
    person_id BIGINT       NOT NULL,
    left_day  DATE         NOT NULL,
    due_total VARCHAR(40)  NOT NULL
);
CREATE INDEX person_snapshot_sheet ON person_snapshot (sheet, id);
