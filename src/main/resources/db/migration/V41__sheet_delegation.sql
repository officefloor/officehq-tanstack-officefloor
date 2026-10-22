-- A steward's delegation of the steward role to a user for a from-date to a to-date, both included, by the sheet's
-- today. While the sheet's today is inside the range the user acts as a steward in every rule, and every audit line
-- of a change they make (not ROLLUP or TOTALS) ends with for=<the steward who delegated>.
CREATE TABLE sheet_delegation (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id     VARCHAR(100) NOT NULL REFERENCES sheet(id),
    user_name    VARCHAR(100) NOT NULL,
    from_date    DATE         NOT NULL,
    to_date      DATE         NOT NULL,
    delegated_by VARCHAR(100) NOT NULL
);
CREATE INDEX sheet_delegation_user ON sheet_delegation(sheet_id, user_name);
