-- A person's approver: a user name who may approve or reject the person's PENDING claims as a steward may (though not
-- one they opened or last edited). A person has at most one approver; setting it again replaces it, blank clears it.
CREATE TABLE person_approver (
    sheet_id   VARCHAR(100) NOT NULL REFERENCES sheet(id),
    person_id  BIGINT       NOT NULL REFERENCES person(id),
    user_name  VARCHAR(100) NOT NULL,
    PRIMARY KEY (sheet_id, person_id)
);
