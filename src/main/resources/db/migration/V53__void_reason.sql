-- A sheet's void reasons: trimmed texts, each on the sheet at most once. While the sheet has any, a claim is voided
-- only with one of them.
CREATE TABLE void_reason (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet (id),
    reason VARCHAR(200) NOT NULL
);
CREATE UNIQUE INDEX void_reason_sheet_reason ON void_reason (sheet_id, reason);
-- The reason a VOID claim was voided with; null when it was voided without one.
ALTER TABLE claim ADD COLUMN void_reason VARCHAR(200);
