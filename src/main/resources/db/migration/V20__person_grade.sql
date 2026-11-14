-- A person's grade is dated: each grade they are given applies from its from-date, and a claim uses
-- the one whose from-date is the latest on or before its date. No from-date applies from the
-- beginning of time: the first grade a person was given. The list shows the grade in force on the
-- sheet's today.
CREATE TABLE person_grade (
    sheet_id  VARCHAR(200) NOT NULL REFERENCES sheet (id),
    person_id BIGINT       NOT NULL REFERENCES person (id),
    grade     VARCHAR(1)   NOT NULL,
    from_date DATE,
    from_key  DATE GENERATED ALWAYS AS (COALESCE(from_date, DATE '0001-01-01')),
    set_by    VARCHAR(200) NOT NULL,
    set_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX person_grade_from ON person_grade (person_id, from_key);
INSERT INTO person_grade (sheet_id, person_id, grade, from_date, set_by)
    SELECT sheet_id, id, grade, NULL, created_by FROM person;
ALTER TABLE person DROP COLUMN grade;
