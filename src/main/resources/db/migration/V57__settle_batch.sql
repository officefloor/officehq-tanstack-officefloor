-- A bulk settle that settles at least one claim is a batch, numbered 1, 2, ... per sheet: each claim it settled keeps
-- its batch number (null when settled one at a time, or never), and the sheet keeps its last batch (0 for none).
ALTER TABLE claim ADD COLUMN settle_batch INT;
ALTER TABLE claim_history ADD COLUMN settle_batch INT;
ALTER TABLE sheet ADD COLUMN last_settle_batch INT DEFAULT 0 NOT NULL;
