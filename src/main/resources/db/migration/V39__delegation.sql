-- A sheet's delegations: a steward (by_name) delegates the steward role to a user name from a from-date to a
-- to-date, inclusive, by the sheet's today.
CREATE TABLE sheet_delegation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet (id),
    user_name VARCHAR(200) NOT NULL,
    by_name VARCHAR(200) NOT NULL,
    from_date DATE NOT NULL,
    to_date DATE NOT NULL
);
