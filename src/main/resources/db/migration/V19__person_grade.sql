-- A person's grade is dated: each row is the grade in force from its from-date until the next one's. A null
-- from-date applies from the beginning of time (the first grade the person was given). At most one row per
-- person and from-date (null included); the store keeps that by replacing the row it already has.
-- person.grade stays as the grade first given, no longer read.
CREATE TABLE person_grade (
    sheet_id  VARCHAR(200) NOT NULL,
    person_id BIGINT       NOT NULL REFERENCES person (id),
    from_date DATE,
    grade     VARCHAR(1)   NOT NULL
);
CREATE INDEX person_grade_person ON person_grade (sheet_id, person_id);
INSERT INTO person_grade (sheet_id, person_id, from_date, grade) SELECT sheet_id, id, NULL, grade FROM person;
