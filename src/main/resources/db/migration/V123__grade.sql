-- The grades added on a sheet beyond A, B and C, each a code of one to three capital letters, listed
-- after them in the order they were added (by id). Every grade column widens to hold such a code.
CREATE TABLE grade (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet(id),
    grade VARCHAR(10) NOT NULL,
    UNIQUE (sheet_id, grade)
);
ALTER TABLE person ALTER COLUMN grade SET DATA TYPE VARCHAR(10);
ALTER TABLE person_grade ALTER COLUMN grade SET DATA TYPE VARCHAR(10);
ALTER TABLE grade_terms ALTER COLUMN grade SET DATA TYPE VARCHAR(10);
ALTER TABLE rate ALTER COLUMN grade SET DATA TYPE VARCHAR(10);
ALTER TABLE rate_history ALTER COLUMN grade SET DATA TYPE VARCHAR(10);
ALTER TABLE person_grade_history ALTER COLUMN grade SET DATA TYPE VARCHAR(10);
ALTER TABLE rate_version ALTER COLUMN grade SET DATA TYPE VARCHAR(10);
