-- The grades retired on a sheet. A retired grade cannot be chosen for a new person or in an edit;
-- people already on it keep it.
CREATE TABLE grade_retired (
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet(id),
    grade VARCHAR(10) NOT NULL,
    PRIMARY KEY (sheet_id, grade)
);
