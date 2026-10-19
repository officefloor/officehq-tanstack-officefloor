-- A person's grade is dated: each grade is in force from its from-date (NULL: from the beginning of
-- time, the first grade the person was given). One grade per person and from-date at most; recording
-- one that exists replaces it. The person's existing grade becomes their first.
CREATE TABLE person_grade (
    sheet_id  VARCHAR(100) NOT NULL,
    person_id BIGINT       NOT NULL,
    grade     CHAR(1)      NOT NULL CHECK (grade IN ('A', 'B', 'C')),
    from_date DATE,
    CONSTRAINT person_grade_from UNIQUE NULLS NOT DISTINCT (sheet_id, person_id, from_date)
);
INSERT INTO person_grade (sheet_id, person_id, grade, from_date) SELECT sheet_id, id, grade, NULL FROM person;
ALTER TABLE person DROP COLUMN grade;
