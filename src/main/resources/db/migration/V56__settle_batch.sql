-- A bulk settle that settles at least one claim (one that goes PENDING counts) is a batch, numbered 1, 2, ... on the
-- sheet in the order the batches happen. The sheet keeps the last batch number it gave out (0: none yet); a claim
-- keeps the number of the batch that settled it (null when settled one at a time, or never settled). Each bulk
-- settle carries a key of its own; settle_batch maps it to the number its first settled claim took.
ALTER TABLE sheet ADD COLUMN last_settle_batch INT DEFAULT 0 NOT NULL;
ALTER TABLE claim ADD COLUMN settle_batch INT;
CREATE TABLE settle_batch (
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet (id),
    batch_key VARCHAR(255) NOT NULL,
    batch_number INT NOT NULL,
    PRIMARY KEY (sheet_id, batch_key)
);
