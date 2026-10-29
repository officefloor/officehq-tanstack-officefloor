-- A person's flag, set by a steward: a reason, in force while the sheet's today is on or before its until-date. While in
-- force, only a steward may open the person's claims. One flag per person: flagging again replaces it. Kept by person id
-- without a reference, so a person leaving the sheet (merged, transferred) leaves only an unread row behind.
CREATE TABLE person_flag (
    person_id  BIGINT       NOT NULL PRIMARY KEY,
    reason     VARCHAR(255) NOT NULL,
    until_date DATE         NOT NULL
);
