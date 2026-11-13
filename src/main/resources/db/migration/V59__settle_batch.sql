-- A bulk settle that settles at least one claim (one going PENDING counts) is a batch, numbered 1, 2, ... on the
-- sheet in the order the batches happen: last_batch is the sheet's last batch number (0: none yet). Each settle of a
-- bulk settle carries the bulk settle's token; the first claim it settles takes the batch number for that token.
-- A claim keeps the number of the batch that settled it (null: settled one at a time, or never settled).
ALTER TABLE sheet ADD COLUMN last_batch INT NOT NULL DEFAULT 0;
CREATE TABLE settle_batch (
    sheet_id VARCHAR(100) NOT NULL REFERENCES sheet (id),
    token VARCHAR(100) NOT NULL,
    number INT NOT NULL,
    PRIMARY KEY (sheet_id, token)
);
ALTER TABLE claim ADD COLUMN batch INT;
