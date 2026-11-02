-- The grades retired on each sheet: a retired grade cannot be chosen for a person; people on it keep it.
CREATE TABLE grade_retired (
    sheet VARCHAR(200) NOT NULL,
    grade VARCHAR(10)  NOT NULL,
    PRIMARY KEY (sheet, grade)
);
