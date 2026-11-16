-- Opening balances pasted onto the sheet: each line adds its amount (0.00 or more) to a person's carried figure for a
-- period, kept in the order pasted with the sheet's today it was made on.
CREATE TABLE opening (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200)   NOT NULL,
    period_id BIGINT         NOT NULL REFERENCES period (id),
    person_id BIGINT         NOT NULL REFERENCES person (id),
    amount    DECIMAL(12, 2) NOT NULL,
    made_on   DATE
);
CREATE INDEX opening_sheet ON opening (sheet_id, period_id);
