-- The people on a sheet, listed in the order they were added (by id). `grade` is one of A, B, C.
CREATE TABLE person (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL,
    name     VARCHAR(500) NOT NULL,
    grade    VARCHAR(1)   NOT NULL
);
CREATE INDEX person_sheet ON person (sheet_id, id);
