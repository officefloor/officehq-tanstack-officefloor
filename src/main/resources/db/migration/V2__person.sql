-- The people on a sheet, listed in the order they were added (by id).
-- name: as entered, never blank; grade: one of A, B or C.
CREATE TABLE person (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(100) NOT NULL,
    name     VARCHAR(200) NOT NULL,
    grade    CHAR(1)      NOT NULL CHECK (grade IN ('A', 'B', 'C'))
);
CREATE INDEX person_sheet ON person (sheet_id, id);
