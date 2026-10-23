-- Each SNAPSHOT written for a person when the sheet's today changes: the day being left and their due-total on it
-- (two decimals, or n/a), in the order written (by id).
CREATE TABLE person_snapshot (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(100) NOT NULL REFERENCES sheet(id),
    person_id BIGINT       NOT NULL,
    left_on   DATE         NOT NULL,
    due_total VARCHAR(20)  NOT NULL
);
CREATE INDEX person_snapshot_sheet ON person_snapshot(sheet_id, person_id, id);
