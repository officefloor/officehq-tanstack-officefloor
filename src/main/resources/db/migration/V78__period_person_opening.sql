-- Each person's opening balance for a period: the sum of the amounts pasted for them in it, added to the
-- figure carried into the period while it is open, and fixed into its carried figure when it is closed.
CREATE TABLE period_person_opening (
    sheet_id    VARCHAR(255)   NOT NULL,
    period_name VARCHAR(255)   NOT NULL,
    person_id   BIGINT         NOT NULL,
    amount      DECIMAL(19, 2) NOT NULL,
    PRIMARY KEY (sheet_id, period_name, person_id)
);
