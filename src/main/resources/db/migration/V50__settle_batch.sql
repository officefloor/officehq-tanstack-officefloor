-- A bulk settle that settles at least one claim (one sent to PENDING counts) is a batch, numbered 1, 2, ... per sheet
-- in the order they happen. Each claim it settled keeps its batch number; null when settled one at a time or never.
CREATE TABLE settle_batch (
    sheet    VARCHAR(200) NOT NULL,
    batch    INTEGER      NOT NULL,
    settled_by VARCHAR(200) NOT NULL,
    PRIMARY KEY (sheet, batch)
);
ALTER TABLE claim ADD COLUMN batch INTEGER;
