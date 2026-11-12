-- A person's flag: put on by a steward with a reason, in force while the sheet's today is on or before its
-- until-date (inclusive). While it is, the row shows the reason and only a steward may open a claim of theirs;
-- after it, it shows nothing and has no effect. A person has at most one: flagging them again replaces it.
CREATE TABLE person_flag (
    sheet_id   VARCHAR(255) NOT NULL,
    person_id  BIGINT       NOT NULL,
    reason     VARCHAR(1000) NOT NULL,
    until_date DATE         NOT NULL,
    PRIMARY KEY (sheet_id, person_id)
);
