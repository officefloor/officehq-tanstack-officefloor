-- A person's grade is dated: person.grade is the first grade they were given, which applies from the
-- beginning of time; each row here is a grade they were given from a date on. At most one per date.
CREATE TABLE person_grade (
    person_id BIGINT     NOT NULL,
    from_date DATE       NOT NULL,
    grade     VARCHAR(1) NOT NULL,
    PRIMARY KEY (person_id, from_date)
);
