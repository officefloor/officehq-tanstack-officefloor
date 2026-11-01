-- A person's grade is dated: one grade per from-date, and a claim uses the one whose from-date is the latest on
-- or before its date. A null from-date (the first grade a person was given) applies from the beginning of time.
-- from_key is the from-date as compared, so there is one grade per person and from-date (null included).
CREATE TABLE person_grade (
    person_id BIGINT     NOT NULL,
    grade     VARCHAR(1) NOT NULL,
    from_date DATE,
    from_key  DATE GENERATED ALWAYS AS (COALESCE(from_date, DATE '0001-01-01'))
);
CREATE UNIQUE INDEX person_grade_person_from ON person_grade (person_id, from_key);
INSERT INTO person_grade (person_id, grade, from_date) SELECT id, grade, NULL FROM person;
ALTER TABLE person DROP COLUMN grade;
