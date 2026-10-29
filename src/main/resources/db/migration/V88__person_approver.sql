-- A person's approver: a user name who may approve or reject the person's PENDING claims as a steward may (four eyes
-- still applies). One approver per person: setting again replaces it. Kept by person id without a reference, like the
-- flag, so a person leaving the sheet leaves only an unread row behind.
CREATE TABLE person_approver (
    person_id BIGINT       NOT NULL PRIMARY KEY,
    user_name VARCHAR(255) NOT NULL
);
