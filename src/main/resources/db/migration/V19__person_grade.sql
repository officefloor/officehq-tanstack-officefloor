-- A person's grade is dated: each grade applies from its from-date on, until the person's next later
-- from-date. The first grade a person was given has no from-date: it applies from the beginning of
-- time. At most one grade per from-date; recording a grade for a from-date again replaces it.
-- A claim uses the grade in force on its date; the list shows the one in force on the sheet's today.
CREATE TABLE person_grade (
    person_id BIGINT     NOT NULL,
    from_date DATE,
    grade     VARCHAR(1) NOT NULL,
    FOREIGN KEY (person_id) REFERENCES person (id)
);
CREATE INDEX person_grade_person ON person_grade (person_id);
INSERT INTO person_grade (person_id, from_date, grade) SELECT id, NULL, grade FROM person;
ALTER TABLE person DROP COLUMN grade;
