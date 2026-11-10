-- The grades added to a sheet (not dated), beside the standing A, B and C: a code of one to three capital letters,
-- listed after them in the order added (by id). Grade columns are widened to hold such a code.
CREATE TABLE sheet_grade (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet (id),
    grade    VARCHAR(3)   NOT NULL,
    UNIQUE (sheet_id, grade)
);
ALTER TABLE person ALTER COLUMN grade SET DATA TYPE VARCHAR(3);
ALTER TABLE person_grade ALTER COLUMN grade SET DATA TYPE VARCHAR(3);
ALTER TABLE rate ALTER COLUMN grade SET DATA TYPE VARCHAR(3);
ALTER TABLE rate_version ALTER COLUMN grade SET DATA TYPE VARCHAR(3);
ALTER TABLE rate_history ALTER COLUMN grade SET DATA TYPE VARCHAR(3);
ALTER TABLE grade_history ALTER COLUMN grade SET DATA TYPE VARCHAR(3);
ALTER TABLE sheet ALTER COLUMN default_grade SET DATA TYPE VARCHAR(3);
