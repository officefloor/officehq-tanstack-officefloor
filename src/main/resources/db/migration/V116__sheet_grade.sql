-- The grades added to a sheet beyond the standing A, B and C, in the order added. A grade is a code of one to
-- three capital letters; every grade column is widened to hold one.
CREATE TABLE sheet_grade (
    seq      BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL,
    grade    VARCHAR(3)   NOT NULL,
    UNIQUE (sheet_id, grade)
);
ALTER TABLE person ALTER COLUMN grade VARCHAR(3) NOT NULL;
ALTER TABLE person_grade ALTER COLUMN grade VARCHAR(3) NOT NULL;
ALTER TABLE grade_history ALTER COLUMN grade VARCHAR(3) NOT NULL;
ALTER TABLE rate ALTER COLUMN grade VARCHAR(3) NOT NULL;
ALTER TABLE rate_version ALTER COLUMN grade VARCHAR(3) NOT NULL;
ALTER TABLE rate_history ALTER COLUMN grade VARCHAR(3) NOT NULL;
ALTER TABLE sheet ALTER COLUMN default_grade VARCHAR(3);
