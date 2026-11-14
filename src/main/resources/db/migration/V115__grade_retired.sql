-- The grades retired on the sheet: such a grade cannot be chosen for a person any more (those on it keep it).
CREATE TABLE grade_retired (
    sheet_id VARCHAR(100) NOT NULL REFERENCES sheet (id),
    grade VARCHAR(10) NOT NULL,
    PRIMARY KEY (sheet_id, grade)
);
