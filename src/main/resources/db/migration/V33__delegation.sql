-- A steward's role delegated to a user for a from- and to-date, both inclusive: while the sheet's today is inside the
-- range the user acts as a steward, on behalf of the steward who delegated. The id keeps the order they were made.
CREATE TABLE delegation (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id   VARCHAR(200) NOT NULL,
    user_name  VARCHAR(200) NOT NULL,
    steward    VARCHAR(200) NOT NULL,
    from_date  DATE         NOT NULL,
    to_date    DATE         NOT NULL
);
