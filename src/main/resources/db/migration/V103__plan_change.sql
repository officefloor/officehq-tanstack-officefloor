-- A plan's amount changes, in the order made: the day (the sheet's today when changed) and the new amount (more
-- than 0.00, two decimals). The plan's amount is kept as the latest; claims generated from then on take it, claims
-- already generated keep theirs.
CREATE TABLE plan_change (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id   VARCHAR(200)   NOT NULL REFERENCES sheet (id),
    plan_id    BIGINT         NOT NULL REFERENCES plan (id),
    changed_on DATE           NOT NULL,
    amount     DECIMAL(14, 2) NOT NULL CHECK (amount > 0)
);
CREATE INDEX plan_change_plan ON plan_change (plan_id, id);
CREATE INDEX plan_change_sheet ON plan_change (sheet_id);
