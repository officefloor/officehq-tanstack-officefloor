-- The people on a sheet. The id is assigned by the store and orders them as they were added.
CREATE TABLE person (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL,
    name     VARCHAR(500) NOT NULL,
    grade    VARCHAR(1)   NOT NULL
);
CREATE INDEX person_sheet ON person (sheet_id, id);
