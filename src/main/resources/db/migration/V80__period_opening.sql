-- A person's opening balance for a period, added to their carried figure for it; pasted openings add up.
CREATE TABLE period_opening (
    sheet VARCHAR(255) NOT NULL,
    period VARCHAR(255) NOT NULL,
    person BIGINT NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    PRIMARY KEY (sheet, period, person)
);
