-- A person's grade is dated: each row is a grade in force from `from_date`, null for the first one
-- the person was given, which applies from the beginning of time. The grade in force on a date is
-- the one with the latest from-date on or before it. Recording a grade for a from-date the person
-- already has replaces it; `from_key` is the from-date as compared for that, with null as the
-- earliest date. `person.grade` stays as the first grade the person was given.
CREATE TABLE person_grade (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200) NOT NULL,
    person_id BIGINT       NOT NULL REFERENCES person (id),
    grade     VARCHAR(1)   NOT NULL,
    from_date DATE,
    from_key  DATE GENERATED ALWAYS AS (COALESCE(from_date, DATE '0001-01-01'))
);
CREATE UNIQUE INDEX person_grade_person_from ON person_grade (person_id, from_key);
CREATE INDEX person_grade_sheet ON person_grade (sheet_id);
INSERT INTO person_grade (sheet_id, person_id, grade, from_date) SELECT sheet_id, id, grade, NULL FROM person;
