-- Every change of a person's own terms, in the order made: the terms before and after (null: none), with the sheet's
-- today when it was made, so the as-of view shows the terms a person had then.
CREATE TABLE person_terms_log (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id    VARCHAR(200) NOT NULL,
    person_id   BIGINT       NOT NULL,
    terms_was   INT,
    terms       INT,
    made_on     DATE         NOT NULL
);
CREATE INDEX person_terms_log_person ON person_terms_log (sheet_id, person_id, id);
