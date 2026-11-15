-- A bulk settle that settles at least one claim is a batch, numbered 1, 2, ... on its sheet; each claim
-- it settled keeps its batch number (null: not settled by a bulk settle).
ALTER TABLE sheet ADD COLUMN last_settle_batch INT;
ALTER TABLE claim ADD COLUMN settle_batch INT;
