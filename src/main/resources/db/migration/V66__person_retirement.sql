-- A person's past retirements, each ended by an unretire: retired from a date to the day they were unretired (the
-- sheet's today then). A claim dated after the from and before the to is refused, as one after a retirement is.
-- retire_made_on is the sheet's today when they were retired (null: not recorded); made_on when unretired.
CREATE TABLE person_retirement (
    person_id BIGINT NOT NULL REFERENCES person (id),
    from_date DATE NOT NULL,
    to_date DATE NOT NULL,
    retire_made_on DATE,
    made_on DATE NOT NULL
);
CREATE INDEX person_retirement_person ON person_retirement (person_id);
