-- A person's grade is dated: each grade they are given is in force from its from-date (the first
-- one, with none, from the beginning of time) until the next one's. A person has at most one grade
-- per from-date (giving one again replaces it). person.grade keeps the grade they were first given;
-- the grade in force on a date is read from here.
CREATE TABLE person_grade (
    sheet_id  VARCHAR(200) NOT NULL REFERENCES sheet (id),
    person_id BIGINT       NOT NULL REFERENCES person (id),
    grade     VARCHAR(1)   NOT NULL,
    from_date DATE
);
CREATE INDEX person_grade_person ON person_grade (person_id, from_date);
INSERT INTO person_grade (sheet_id, person_id, grade, from_date) SELECT sheet_id, id, grade, NULL FROM person;
