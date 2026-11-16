-- The grades added to a sheet beyond A, B and C: a code of one to three capital letters, listed after
-- them in the order added. A person's grade and a rate's grade may be any grade of their sheet.
CREATE TABLE grade_added (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200) NOT NULL REFERENCES sheet (id),
    grade     VARCHAR(10)  NOT NULL,
    added_by  VARCHAR(200) NOT NULL,
    added_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT grade_added_unique UNIQUE (sheet_id, grade)
);
ALTER TABLE person_grade ALTER COLUMN grade VARCHAR(10) NOT NULL;
ALTER TABLE rate ALTER COLUMN grade VARCHAR(10) NOT NULL;
