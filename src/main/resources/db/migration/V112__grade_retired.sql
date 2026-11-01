-- The grades retired on each sheet: a retired grade cannot be chosen for a person (added, edited,
-- imported or quick-added), though the people already on it keep it. A grade is retired at most once.
CREATE TABLE grade_retired (
    sheet_id VARCHAR(200) NOT NULL,
    grade    VARCHAR(10)  NOT NULL,
    PRIMARY KEY (sheet_id, grade)
);
