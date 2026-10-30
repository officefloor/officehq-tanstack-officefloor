-- Each person's SNAPSHOT lines: their due-total as shown ("n/a" for none) on the day being left
-- (set_on, the sheet's today before it moved), one per SNAPSHOT written, in the order written (seq).
CREATE TABLE person_snapshot (
    seq       BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200) NOT NULL,
    person_id BIGINT       NOT NULL,
    due_total VARCHAR(40)  NOT NULL,
    set_on    DATE
);
