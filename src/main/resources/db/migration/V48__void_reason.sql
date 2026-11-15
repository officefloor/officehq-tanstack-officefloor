-- The sheet's void reasons: while it has any, a claim is voided with one of them.
CREATE TABLE void_reason (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id    VARCHAR(200) NOT NULL REFERENCES sheet (id),
    reason      VARCHAR(200) NOT NULL,
    added_by    VARCHAR(200) NOT NULL,
    added_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT void_reason_unique UNIQUE (sheet_id, reason)
);
-- The reason a claim was voided with (none: voided without one, or not voided).
ALTER TABLE claim ADD COLUMN void_reason VARCHAR(200);
