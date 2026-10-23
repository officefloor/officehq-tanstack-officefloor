-- The grades retired on a sheet: people already on one keep it, but it is not given to anyone again.
CREATE TABLE sheet_retired_grade (
    sheet_id   VARCHAR(100) NOT NULL REFERENCES sheet(id),
    grade      VARCHAR(10)  NOT NULL,
    retired_by VARCHAR(100) NOT NULL,
    PRIMARY KEY (sheet_id, grade)
);
