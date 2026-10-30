-- A person's past retirements, each from its from-date to the sheet's today when they were
-- unretired, in the order they ended (seq). No claim of theirs may be dated after the from-date
-- and before the to-date of one.
CREATE TABLE person_retirement (
    seq       BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200) NOT NULL,
    person_id BIGINT       NOT NULL,
    from_date DATE         NOT NULL,
    to_date   DATE         NOT NULL,
    set_on    DATE
);
