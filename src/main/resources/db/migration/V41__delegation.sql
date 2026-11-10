-- A steward's delegation of the steward role to a user for a from-date and a to-date, inclusive, by the
-- sheet's today. The id keeps the order they were made in.
CREATE TABLE sheet_delegation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet(id),
    user_name VARCHAR(200) NOT NULL,
    by_user VARCHAR(200) NOT NULL,
    from_date DATE NOT NULL,
    to_date DATE NOT NULL
);
CREATE INDEX sheet_delegation_sheet ON sheet_delegation(sheet_id, user_name, id);
