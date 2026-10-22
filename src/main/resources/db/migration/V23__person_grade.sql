-- A person's grade is dated: each grade they are given applies from its from-date (null: from the beginning of
-- time, the first grade a person was given), at most one per from-date. A claim uses the grade whose from-date is
-- the latest on or before its date; the list shows the one in force on the sheet's today.
CREATE TABLE person_grade (
    sheet_id  VARCHAR(100) NOT NULL REFERENCES sheet(id),
    person_id BIGINT       NOT NULL REFERENCES person(id),
    grade     CHAR(1)      NOT NULL CHECK (grade IN ('A', 'B', 'C')),
    from_date DATE
);
CREATE INDEX person_grade_sheet ON person_grade(sheet_id, person_id);
INSERT INTO person_grade (sheet_id, person_id, grade, from_date) SELECT sheet_id, id, grade, NULL FROM person;
ALTER TABLE person DROP COLUMN grade;
