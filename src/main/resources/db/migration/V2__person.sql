-- People on a sheet, listed in the order they were added (by id).
CREATE TABLE person (
    id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet VARCHAR(200) NOT NULL,
    name  VARCHAR(200) NOT NULL,
    grade VARCHAR(1)   NOT NULL
);
CREATE INDEX person_sheet ON person (sheet, id);
