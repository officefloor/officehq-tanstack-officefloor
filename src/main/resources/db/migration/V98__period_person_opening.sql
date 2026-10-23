-- Opening balances pasted onto a sheet: one row per pasted line, adding its amount (0.00 or more) to the person's
-- carried figure for the period. A person's opening for a period is their rows added up; it is part of their live
-- carried figure, and of the one fixed when the period is closed.
CREATE TABLE period_person_opening (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id    VARCHAR(100)   NOT NULL REFERENCES sheet(id),
    period_name VARCHAR(200)   NOT NULL,
    person_id   BIGINT         NOT NULL REFERENCES person(id),
    amount      DECIMAL(14, 2) NOT NULL
);
CREATE INDEX period_person_opening_sheet ON period_person_opening (sheet_id);
