-- The sheet's void reasons, in the order they were added: a reason is on a sheet's list at most once. While the list
-- is not empty, a claim is voided with a reason chosen from it, kept on the claim (null: voided with none).
CREATE TABLE void_reason (
    sheet_id VARCHAR(100) NOT NULL REFERENCES sheet (id),
    reason VARCHAR(200) NOT NULL,
    position INT NOT NULL,
    PRIMARY KEY (sheet_id, reason)
);
ALTER TABLE claim ADD COLUMN void_reason VARCHAR(200);
