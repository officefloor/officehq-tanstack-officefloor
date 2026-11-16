-- Each bulk settle that settled at least one claim (one made PENDING counts): a batch, numbered 1, 2, ... per sheet in
-- the order they happen, with the sheet's today when it was made.
CREATE TABLE settle_batch (
    sheet_id VARCHAR(200) NOT NULL,
    batch    INT          NOT NULL,
    made_on  DATE         NOT NULL,
    PRIMARY KEY (sheet_id, batch)
);

-- The batch that settled a claim; a claim settled one at a time has no row (settling it so removes any it had).
CREATE TABLE claim_batch (
    sheet_id VARCHAR(200) NOT NULL,
    claim_id BIGINT       NOT NULL REFERENCES claim (id),
    batch    INT          NOT NULL,
    PRIMARY KEY (sheet_id, claim_id)
);
