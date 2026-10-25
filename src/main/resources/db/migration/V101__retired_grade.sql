-- A sheet's retired grades: each at most once. People on one keep it, but it cannot be given to a new person or in an
-- edit.
CREATE TABLE retired_grade (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet (id),
    grade VARCHAR(10) NOT NULL
);
CREATE UNIQUE INDEX retired_grade_sheet_grade ON retired_grade (sheet_id, grade);
