-- Each person's opening balance for a period, added to their carried figure for it (pasted openings add up).
CREATE TABLE period_person_opening (
    period_id BIGINT NOT NULL REFERENCES period (id),
    person_id BIGINT NOT NULL,
    opening DECIMAL(19, 2) NOT NULL,
    PRIMARY KEY (period_id, person_id)
);
