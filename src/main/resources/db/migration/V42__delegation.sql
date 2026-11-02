-- A steward's delegation of the steward role to a user, from its from-date to its to-date (both inclusive).
-- While the sheet's today is inside the range the user acts as a steward in every rule, and each audit line
-- of a change they make (not one that only reports figures) ends with for=<the steward who delegated>.
CREATE TABLE delegation (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200) NOT NULL REFERENCES sheet (id),
    user_name VARCHAR(200) NOT NULL,
    by_user   VARCHAR(200) NOT NULL,
    from_date DATE         NOT NULL,
    to_date   DATE         NOT NULL
);
CREATE INDEX delegation_user ON delegation (sheet_id, user_name);
