-- A person's own terms can be dated: each applies from its from-date until the next. Their terms in
-- person.terms apply from the beginning of time, until the first from-date here. Terms null here mean
-- none of their own from that date (then the grade's or the sheet's). Recording terms for a from-date
-- that already has some replaces them. Kept by version like the rest of the screen.
CREATE TABLE person_terms (
    sheet_id  VARCHAR(255) NOT NULL,
    person_id BIGINT       NOT NULL,
    from_date DATE         NOT NULL,
    terms     INT,
    PRIMARY KEY (person_id, from_date)
);
CREATE INDEX person_terms_sheet ON person_terms (sheet_id, person_id);

CREATE TABLE person_terms_hist AS SELECT * FROM person_terms WITH NO DATA;
ALTER TABLE person_terms_hist ADD COLUMN hist_seq BIGINT NOT NULL;
ALTER TABLE person_terms_hist ADD COLUMN hist_made_on DATE NOT NULL;
ALTER TABLE person_terms_hist ADD COLUMN hist_gone BOOLEAN DEFAULT FALSE NOT NULL;
CREATE INDEX person_terms_hist_key ON person_terms_hist (person_id, from_date, hist_seq);
CREATE TRIGGER person_terms_history AFTER INSERT, UPDATE, DELETE ON person_terms FOR EACH ROW
    CALL 'net.officefloor.hq.app.history.HistoryTrigger';
