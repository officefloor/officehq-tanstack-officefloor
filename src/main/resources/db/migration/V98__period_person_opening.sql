-- Each person's opening balance for a period: pasted amounts added up, added to the person's
-- carried figure for that period. A person with no row has no opening balance for it.
CREATE TABLE period_person_opening (
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet(id),
    period_name VARCHAR(200) NOT NULL,
    person_id BIGINT NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    PRIMARY KEY (sheet_id, period_name, person_id)
);
