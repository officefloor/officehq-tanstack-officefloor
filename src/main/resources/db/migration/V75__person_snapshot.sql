-- Every SNAPSHOT written for a person, in the order written: the day being left and their due-total on it (two
-- decimals, or n/a), so each person shows their snapshot lines.
CREATE TABLE person_snapshot (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id   VARCHAR(200) NOT NULL,
    person_id  BIGINT       NOT NULL,
    left_on    DATE         NOT NULL,
    due_total  VARCHAR(20)  NOT NULL
);
CREATE INDEX person_snapshot_sheet ON person_snapshot (sheet_id, id);
