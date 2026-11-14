-- People kept on a sheet, listed in the order they were added (by id).
CREATE TABLE person (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id   VARCHAR(200) NOT NULL REFERENCES sheet (id),
    name       VARCHAR(200) NOT NULL,
    grade      VARCHAR(1)   NOT NULL,
    created_by VARCHAR(200) NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX person_sheet ON person (sheet_id, id);
