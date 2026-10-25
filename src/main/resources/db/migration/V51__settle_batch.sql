-- A bulk settle that settles at least one claim (one left PENDING counts) is a batch, numbered 1, 2, ... on the sheet
-- in the order the batches happen: the sheet keeps its last batch number (0: none yet), and each claim settled by one
-- keeps its number (null: not settled by a batch).
ALTER TABLE sheet ADD COLUMN last_settle_batch INT NOT NULL DEFAULT 0;
ALTER TABLE hist.sheet ADD COLUMN last_settle_batch INT;
ALTER TABLE asof.sheet ADD COLUMN last_settle_batch INT;
ALTER TABLE claim ADD COLUMN settle_batch INT;
ALTER TABLE hist.claim ADD COLUMN settle_batch INT;
ALTER TABLE asof.claim ADD COLUMN settle_batch INT;
