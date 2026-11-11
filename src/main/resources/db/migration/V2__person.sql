-- The people on a sheet, listed in the order they were added (by id).
CREATE TABLE person (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(255) NOT NULL,
    name     VARCHAR(255) NOT NULL,
    grade    VARCHAR(1)   NOT NULL
);
CREATE INDEX person_sheet ON person (sheet_id, id);
