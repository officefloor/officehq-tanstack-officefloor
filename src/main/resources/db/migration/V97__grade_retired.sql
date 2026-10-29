-- The grades retired on a sheet, each at most once: a retired grade cannot be chosen for a person (people on it keep it).
CREATE TABLE grade_retired (
    sheet_id VARCHAR(255) NOT NULL,
    grade    VARCHAR(1)   NOT NULL,
    PRIMARY KEY (sheet_id, grade)
);
