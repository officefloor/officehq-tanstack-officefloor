-- The grades retired on a sheet: a retired grade cannot be chosen for anyone, though people on it keep it.
CREATE TABLE retired_grade (
    sheet_id VARCHAR(200) NOT NULL,
    grade    VARCHAR(20)  NOT NULL,
    PRIMARY KEY (sheet_id, grade)
);
