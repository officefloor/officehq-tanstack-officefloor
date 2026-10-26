-- Opening balances: what a person carried into a period before the sheet kept it, pasted as name,period,amount. Each
-- pasted line adds its amount to the person's opening for the period, which the carried figure for it adds on top.
CREATE TABLE period_opening (
    sheet_id  VARCHAR(200)   NOT NULL REFERENCES sheet (id),
    period_id BIGINT         NOT NULL REFERENCES period (id),
    person_id BIGINT         NOT NULL REFERENCES person (id),
    amount    DECIMAL(14, 2) NOT NULL,
    PRIMARY KEY (period_id, person_id)
);

-- Snapshotted with the rest of the screen (see V40, V41).
CREATE TABLE hist.period_opening AS SELECT t.*, CAST(NULL AS BIGINT) AS snapshot_id FROM period_opening t WHERE FALSE;
CREATE INDEX hist_period_opening_snapshot ON hist.period_opening (snapshot_id);
CREATE TABLE asof.period_opening AS SELECT * FROM period_opening WHERE FALSE;
