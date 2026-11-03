-- Settle batches: a bulk settle that settles (or sends to PENDING) at least one claim is the sheet's next
-- batch, numbered 1, 2, ... in the order they happen. The sheet keeps the number of its last batch (null
-- when there has been none), and each claim settled by a batch keeps its number (null when settled one at a time).
ALTER TABLE sheet ADD COLUMN last_batch INT;
ALTER TABLE claim ADD COLUMN batch INT;
