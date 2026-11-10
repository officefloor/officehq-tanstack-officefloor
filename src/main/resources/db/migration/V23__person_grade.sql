-- A person's grade is dated: person.grade is the first grade they were given, in force from the
-- beginning of time; each row here is a grade in force from its date, until a later one. At most
-- one row per person and from-date, kept by PersonRepository.save.
CREATE TABLE person_grade (
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet(id),
    person_id BIGINT NOT NULL REFERENCES person(id),
    from_date DATE NOT NULL,
    grade CHAR(1) NOT NULL,
    PRIMARY KEY (person_id, from_date)
);
CREATE INDEX person_grade_sheet ON person_grade(sheet_id, person_id);
