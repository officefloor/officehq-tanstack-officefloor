-- Each fiscal-year rollover made on a sheet: the old year, who made it and the sheet's today then,
-- and whether it has been undone. The last one (highest id) is the one an undo takes back.
CREATE TABLE rollover (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet(id),
    fy VARCHAR(20) NOT NULL,
    made_by VARCHAR(200) NOT NULL,
    made_on DATE NOT NULL,
    undone BOOLEAN DEFAULT FALSE NOT NULL
);
-- The periods a rollover generated.
CREATE TABLE rollover_period (
    rollover_id BIGINT NOT NULL REFERENCES rollover(id),
    period_name VARCHAR(200) NOT NULL
);
-- The opening balances a rollover added: the amount added to the person's opening for the period.
CREATE TABLE rollover_opening (
    rollover_id BIGINT NOT NULL REFERENCES rollover(id),
    period_name VARCHAR(200) NOT NULL,
    person_id BIGINT NOT NULL,
    amount DECIMAL(19, 2) NOT NULL
);
