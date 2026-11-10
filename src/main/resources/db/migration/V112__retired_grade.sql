-- The sheet's retired grades (not dated): a retired grade cannot be chosen for a person any more; people already on
-- it keep it.
CREATE TABLE retired_grade (
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet (id),
    grade    VARCHAR(20)  NOT NULL,
    PRIMARY KEY (sheet_id, grade)
);
