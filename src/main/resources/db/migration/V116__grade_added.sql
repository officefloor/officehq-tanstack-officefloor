-- The grades added on the sheet beyond A, B and C, in the order they were added (position 1, 2, ...).
CREATE TABLE grade_added (
    sheet_id VARCHAR(100) NOT NULL REFERENCES sheet (id),
    grade VARCHAR(10) NOT NULL,
    position INT NOT NULL,
    PRIMARY KEY (sheet_id, grade)
);

-- An added grade is up to three letters: a person's grades hold it as the rates do.
ALTER TABLE person ALTER COLUMN grade SET DATA TYPE VARCHAR(10);
ALTER TABLE person_grade ALTER COLUMN grade SET DATA TYPE VARCHAR(10);
