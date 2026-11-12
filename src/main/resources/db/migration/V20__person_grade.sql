-- A person's grade is dated: each applies from its from-date until the next. The first grade a
-- person was given applies from the beginning of time, kept as 0001-01-01. Recording a grade for a
-- from-date that already has one replaces it.
CREATE TABLE person_grade (
    sheet_id  VARCHAR(255) NOT NULL,
    person_id BIGINT       NOT NULL,
    from_date DATE         NOT NULL,
    grade     VARCHAR(1)   NOT NULL,
    PRIMARY KEY (person_id, from_date)
);
CREATE INDEX person_grade_sheet ON person_grade (sheet_id, person_id);
INSERT INTO person_grade (sheet_id, person_id, from_date, grade)
    SELECT sheet_id, id, DATE '0001-01-01', grade FROM person;
ALTER TABLE person DROP COLUMN grade;
