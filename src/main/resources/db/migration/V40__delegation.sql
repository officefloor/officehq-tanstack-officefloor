-- A steward's delegation of the steward role to a user, kept per sheet: while the sheet's today is
-- from from_date to to_date (inclusive) the user acts as a steward, for the steward who delegated.
CREATE TABLE sheet_delegation (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200) NOT NULL,
    user_name VARCHAR(200) NOT NULL,
    from_date DATE         NOT NULL,
    to_date   DATE         NOT NULL,
    by_user   VARCHAR(200) NOT NULL
);
CREATE INDEX sheet_delegation_sheet ON sheet_delegation (sheet_id, user_name, id);
