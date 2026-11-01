-- A steward's role lent to a user from a from-date to a to-date, both inclusive, by the sheet's today; the id keeps
-- the order they were made.
CREATE TABLE delegation (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet      VARCHAR(200) NOT NULL,
    delegator  VARCHAR(200) NOT NULL,
    user_name  VARCHAR(200) NOT NULL,
    from_date  DATE         NOT NULL,
    to_date    DATE         NOT NULL
);
