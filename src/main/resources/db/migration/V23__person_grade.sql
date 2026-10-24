-- A person's dated grades: each save of the person form records its grade from its from-date, replacing the
-- one recorded for that date. The person's own grade is the first one they were given, which applies from the
-- beginning of time.
CREATE TABLE person_grade (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet (id),
    person_id BIGINT NOT NULL REFERENCES person (id),
    grade VARCHAR(10) NOT NULL,
    from_date DATE NOT NULL,
    CONSTRAINT person_grade_unique UNIQUE (person_id, from_date)
);
CREATE INDEX person_grade_sheet ON person_grade (sheet_id, id);
