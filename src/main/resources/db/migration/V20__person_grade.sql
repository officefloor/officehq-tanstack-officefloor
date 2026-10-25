-- A person's grade is dated: each grade they are given applies from its from-date, the first one
-- (null) from the beginning of time. On a date, the grade in force is the one with the latest
-- from-date on or before it. A person has at most one grade per from-date; giving one again on the
-- same date replaces it.
CREATE TABLE person_grade (
    person_id BIGINT     NOT NULL REFERENCES person (id),
    grade     VARCHAR(1) NOT NULL,
    from_date DATE
);
CREATE INDEX person_grade_person ON person_grade (person_id, from_date);
INSERT INTO person_grade (person_id, grade, from_date) SELECT id, grade, NULL FROM person;
ALTER TABLE person DROP COLUMN grade;
