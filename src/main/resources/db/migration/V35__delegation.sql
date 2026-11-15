-- A steward delegates the steward role to a user for a from-date to a to-date, inclusive, by the
-- sheet's today. While the sheet's today is inside the range the user acts as a steward in every rule,
-- and the audit lines of a change they make name the steward who delegated (for=).
CREATE TABLE delegation (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id     VARCHAR(200) NOT NULL REFERENCES sheet (id),
    user_name    VARCHAR(200) NOT NULL,
    from_date    DATE         NOT NULL,
    to_date      DATE         NOT NULL,
    delegated_by VARCHAR(200) NOT NULL,
    delegated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX delegation_user ON delegation (sheet_id, user_name);
