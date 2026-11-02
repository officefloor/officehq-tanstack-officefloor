-- The grades added on each sheet beyond A, B and C, in the order they were added.
CREATE TABLE grade_added (
    id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet VARCHAR(200) NOT NULL,
    grade VARCHAR(10)  NOT NULL,
    UNIQUE (sheet, grade)
);

-- An added grade has up to three letters: widen every column holding a grade.
ALTER TABLE person_grade ALTER COLUMN grade SET DATA TYPE VARCHAR(10);
ALTER TABLE rate ALTER COLUMN grade SET DATA TYPE VARCHAR(10);
ALTER TABLE rate_version ALTER COLUMN grade SET DATA TYPE VARCHAR(10);
