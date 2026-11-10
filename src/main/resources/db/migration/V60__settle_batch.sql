-- A bulk settle that settles at least one claim is a batch, numbered 1, 2, ... on its sheet: the
-- sheet keeps its last batch number (0: none yet), each claim the batch that settled it (null:
-- none did), and its history too, so the as-of view shows the batches as they stood then.
ALTER TABLE sheet ADD COLUMN last_settle_batch INT NOT NULL DEFAULT 0;
ALTER TABLE claim ADD COLUMN settle_batch INT;
ALTER TABLE claim_history ADD COLUMN settle_batch INT;
