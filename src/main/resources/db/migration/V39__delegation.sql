-- A steward's delegation of the steward role to a user for a from-date and a to-date, inclusive, by the sheet's
-- today: while the sheet's today is inside the range the user acts as a steward, and the audit lines of their
-- changes end with for=<the steward who delegated>. Kept in the order given (seq).
CREATE TABLE sheet_delegation (
    seq          BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id     VARCHAR(200) NOT NULL,
    user_name    VARCHAR(200) NOT NULL,
    from_date    DATE         NOT NULL,
    to_date      DATE         NOT NULL,
    delegated_by VARCHAR(200) NOT NULL
);
