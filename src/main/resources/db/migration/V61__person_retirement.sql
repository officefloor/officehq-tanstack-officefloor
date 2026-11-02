-- A person's past retirements: each from the retired-from date it had to the day they were unretired, listed in the
-- order they were unretired (by id). A claim dated after the from and before the to of one is refused.
CREATE TABLE person_retirement (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    person_id BIGINT NOT NULL,
    from_date DATE   NOT NULL,
    to_date   DATE   NOT NULL
);
CREATE INDEX person_retirement_person ON person_retirement (person_id, id);
