-- A steward's flag on a person: a reason and the date it runs until (inclusive). A person has at most one flag; flagging
-- again replaces it. While the sheet's today is on or before the until date, only a steward may open their claims.
CREATE TABLE person_flag (
    sheet_id   VARCHAR(100) NOT NULL REFERENCES sheet(id),
    person_id  BIGINT       NOT NULL REFERENCES person(id),
    reason     VARCHAR(200) NOT NULL,
    until_date DATE         NOT NULL,
    PRIMARY KEY (sheet_id, person_id)
);
