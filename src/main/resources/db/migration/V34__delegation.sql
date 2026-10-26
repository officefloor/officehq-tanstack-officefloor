-- A steward's delegation of the steward role to a user name, from a from-date to a to-date (inclusive). While the
-- sheet's today is inside the range the user acts as a steward; delegated_by is the steward who delegated.
CREATE TABLE delegation (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id     VARCHAR(255) NOT NULL,
    user_name    VARCHAR(255) NOT NULL,
    from_date    DATE         NOT NULL,
    to_date      DATE         NOT NULL,
    delegated_by VARCHAR(255) NOT NULL
);
CREATE INDEX delegation_sheet ON delegation (sheet_id, user_name);
