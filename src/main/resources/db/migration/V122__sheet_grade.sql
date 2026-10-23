-- The grades added to a sheet beyond A, B and C: a code of one to three capital letters, on a sheet at most once.
-- They follow A, B and C in the order added.
CREATE TABLE sheet_grade (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(100) NOT NULL REFERENCES sheet(id),
    grade    VARCHAR(3)   NOT NULL,
    added_by VARCHAR(100) NOT NULL,
    CONSTRAINT sheet_grade_code UNIQUE (sheet_id, grade)
);

-- A grade is now up to three letters: every column holding one is widened.
ALTER TABLE rate ALTER COLUMN grade VARCHAR(3) NOT NULL;
ALTER TABLE rate_log ALTER COLUMN grade VARCHAR(3) NOT NULL;
ALTER TABLE person_grade ALTER COLUMN grade VARCHAR(3) NOT NULL;
ALTER TABLE person_grade_log ALTER COLUMN grade VARCHAR(3) NOT NULL;
ALTER TABLE sheet ALTER COLUMN default_grade VARCHAR(3);
