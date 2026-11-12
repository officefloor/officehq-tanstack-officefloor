-- A person's approver: a user who may approve or reject the person's PENDING claims as a steward may (not those
-- they opened or last edited). A person has at most one: setting it again replaces it, a blank one clears it.
CREATE TABLE person_approver (
    sheet_id  VARCHAR(255) NOT NULL,
    person_id BIGINT       NOT NULL,
    approver  VARCHAR(255) NOT NULL,
    PRIMARY KEY (sheet_id, person_id)
);
