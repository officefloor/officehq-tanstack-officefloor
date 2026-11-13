-- The steward role delegated by a steward (by_user) to a user name on a sheet, from a from-date to a to-date, both
-- inclusive: while the sheet's today is inside them the user acts as a steward. The id keeps the order they were made.
CREATE TABLE delegation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(100) NOT NULL REFERENCES sheet (id),
    user_name VARCHAR(100) NOT NULL,
    from_date DATE NOT NULL,
    to_date DATE NOT NULL,
    by_user VARCHAR(100) NOT NULL
);
CREATE INDEX delegation_sheet ON delegation (sheet_id, user_name);
