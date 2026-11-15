-- The statements a transfer dropped from a person, kept by the sheet they left so that undoing the
-- transfer gives them back there: their counters (person_left) and each statement (person_statement_left).
CREATE TABLE person_left (
    sheet_id        VARCHAR(200) NOT NULL,
    person_id       BIGINT       NOT NULL,
    statement_count INT          NOT NULL,
    stated_total    VARCHAR(30),
    statement_date  DATE,
    PRIMARY KEY (sheet_id, person_id)
);
CREATE TABLE person_statement_left (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id       VARCHAR(200) NOT NULL,
    person_id      BIGINT       NOT NULL,
    stated_total   VARCHAR(30),
    statement_date DATE         NOT NULL,
    taken_at       TIMESTAMP    NOT NULL
);
CREATE INDEX person_statement_left_person ON person_statement_left (person_id, sheet_id, id);
