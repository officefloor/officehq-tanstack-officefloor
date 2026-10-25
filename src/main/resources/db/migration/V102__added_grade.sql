-- A sheet's added grades, beyond A, B and C: each a code of one to three capital letters, at most once, listed in the
-- order added (by id) after A, B and C.
CREATE TABLE added_grade (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet (id),
    grade VARCHAR(10) NOT NULL
);
CREATE UNIQUE INDEX added_grade_sheet_grade ON added_grade (sheet_id, grade);
ALTER TABLE person ALTER COLUMN grade VARCHAR(10);
ALTER TABLE rate ALTER COLUMN grade VARCHAR(10);
