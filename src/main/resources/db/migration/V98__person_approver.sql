-- A person's approver: a user who may approve or reject the person's PENDING claims as a steward may. A person has
-- at most one approver; none: no row.
CREATE TABLE person_approver (
    sheet_id   VARCHAR(200) NOT NULL,
    person_id  BIGINT       NOT NULL PRIMARY KEY REFERENCES person (id),
    user_name  VARCHAR(200) NOT NULL
);
