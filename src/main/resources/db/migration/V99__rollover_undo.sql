-- The last year end rollover on a sheet, kept so the steward who made it can undo it on the same sheet-today: who made
-- it, on which sheet-today, of which fiscal year, the periods it generated and the opening balances it set.
CREATE TABLE rollover (
    sheet VARCHAR(255) NOT NULL PRIMARY KEY,
    fy VARCHAR(20) NOT NULL,
    made_by VARCHAR(200) NOT NULL,
    made_on DATE NOT NULL
);
CREATE TABLE rollover_period (
    sheet VARCHAR(255) NOT NULL,
    period VARCHAR(255) NOT NULL,
    PRIMARY KEY (sheet, period)
);
CREATE TABLE rollover_opening (
    sheet VARCHAR(255) NOT NULL,
    period VARCHAR(255) NOT NULL,
    person BIGINT NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    PRIMARY KEY (sheet, period, person)
);
