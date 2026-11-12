-- A person's aliases: other names they are found by, stored trimmed. Aliases share the names'
-- uniqueness on a sheet, without regard to case: no alias equals another alias or any person's name
-- (the app checks the names; the index the aliases). Kept in person_alias_hist, as person_hold is.
CREATE TABLE person_alias (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(255) NOT NULL,
    person_id BIGINT       NOT NULL,
    alias     VARCHAR(255) NOT NULL,
    alias_key VARCHAR(255) GENERATED ALWAYS AS (LOWER(alias))
);
CREATE UNIQUE INDEX person_alias_sheet_key ON person_alias (sheet_id, alias_key);
CREATE INDEX person_alias_person ON person_alias (sheet_id, person_id);

CREATE TABLE person_alias_hist AS SELECT * FROM person_alias WITH NO DATA;
ALTER TABLE person_alias_hist ADD COLUMN hist_seq BIGINT NOT NULL;
ALTER TABLE person_alias_hist ADD COLUMN hist_made_on DATE NOT NULL;
ALTER TABLE person_alias_hist ADD COLUMN hist_gone BOOLEAN DEFAULT FALSE NOT NULL;
CREATE INDEX person_alias_hist_key ON person_alias_hist (id, hist_seq);
CREATE TRIGGER person_alias_history AFTER INSERT, UPDATE, DELETE ON person_alias FOR EACH ROW
    CALL 'net.officefloor.hq.app.history.HistoryTrigger';
