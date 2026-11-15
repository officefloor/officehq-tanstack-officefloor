-- Each SNAPSHOT written for a person, in the order written: the day being left (the sheet's today
-- before it changed) and their due-total on it (text: two decimals, or n/a).
CREATE TABLE person_snapshot (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200) NOT NULL REFERENCES sheet (id),
    person_id BIGINT       NOT NULL,
    day_left  DATE         NOT NULL,
    due_total VARCHAR(30)  NOT NULL
);
CREATE INDEX person_snapshot_person ON person_snapshot (sheet_id, person_id, id);
