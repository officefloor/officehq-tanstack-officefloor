-- A person's grade is dated: each row is a grade in force from its from-date until the next one.
-- A null from-date applies from the beginning of time (the first grade the person was given). A
-- claim uses the grade in force on its date; the list shows the one in force on the sheet's today.
-- One grade per (sheet, person, from-date), a null from-date counting as one value.
CREATE TABLE person_grade (
    seq       BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200) NOT NULL,
    person_id BIGINT       NOT NULL,
    from_date DATE,
    grade     CHAR(1)      NOT NULL
);
CREATE INDEX person_grade_person ON person_grade (sheet_id, person_id);
INSERT INTO person_grade (sheet_id, person_id, from_date, grade) SELECT sheet_id, id, NULL, grade FROM person;
