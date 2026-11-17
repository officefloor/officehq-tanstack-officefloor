-- The grades retired on each sheet: a retired grade cannot be chosen for a person (see GradeRepository); people already
-- on it keep it.
CREATE TABLE grade_retired (
    sheet_id VARCHAR(200) NOT NULL,
    grade    VARCHAR(20)  NOT NULL,
    PRIMARY KEY (sheet_id, grade)
);
