-- The steward role delegated to a user by a steward for from..to (inclusive), by the sheet's today.
-- The id orders a sheet's delegations as they were made.
CREATE TABLE delegation (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id    VARCHAR(255) NOT NULL,
    user_name   VARCHAR(255) NOT NULL,
    from_date   DATE         NOT NULL,
    to_date     DATE         NOT NULL,
    delegated_by VARCHAR(255) NOT NULL
);
