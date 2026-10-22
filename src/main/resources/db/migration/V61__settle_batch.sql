-- A bulk settle that settles at least one claim (one left PENDING counts) is a batch, numbered 1, 2, ... on its sheet
-- in the order the batches happen.
CREATE TABLE settle_batch (
    sheet_id VARCHAR(100) NOT NULL,
    batch INT NOT NULL,
    PRIMARY KEY (sheet_id, batch)
);

-- The batch that settled each claim a bulk settle settled; a claim settled one at a time has none.
CREATE TABLE claim_batch (
    sheet_id VARCHAR(100) NOT NULL,
    claim_id BIGINT NOT NULL,
    batch INT NOT NULL,
    PRIMARY KEY (sheet_id, claim_id)
);
