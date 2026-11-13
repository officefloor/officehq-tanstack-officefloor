-- A person's grades, each recorded from a from-date (null = from the beginning of time: the first grade
-- the person was given). The grade in force on a date is the one with the latest from-date on or before
-- it; person.grade keeps only the first grade given.
CREATE TABLE person_grade (
    sheet_id VARCHAR(100) NOT NULL REFERENCES sheet (id),
    person_id BIGINT NOT NULL REFERENCES person (id),
    grade VARCHAR(1) NOT NULL,
    from_date DATE
);
CREATE INDEX person_grade_sheet ON person_grade (sheet_id, person_id);
INSERT INTO person_grade (sheet_id, person_id, grade, from_date) SELECT sheet_id, id, grade, NULL FROM person;
