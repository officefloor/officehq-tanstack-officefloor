-- A steward's delegation of the steward role to a user name, for a from-date to a to-date (inclusive) by the sheet's
-- today. While the sheet's today is inside the range the user acts as a steward, for the steward who delegated.
CREATE TABLE sheet_delegation (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200) NOT NULL REFERENCES sheet (id),
    user_name VARCHAR(200) NOT NULL,
    from_date DATE         NOT NULL,
    to_date   DATE         NOT NULL,
    by_user   VARCHAR(200) NOT NULL
);
CREATE INDEX sheet_delegation_user ON sheet_delegation (sheet_id, user_name);
