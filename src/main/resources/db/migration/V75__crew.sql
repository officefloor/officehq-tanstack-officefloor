-- The sheet's crews: a name (trimmed, at most once per sheet), a lead (a person on the sheet) and its members,
-- each a person on the sheet at most once per crew and never the crew's lead. Kept by version like the rest
-- of the screen.
CREATE TABLE crew (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(255)  NOT NULL,
    name      VARCHAR(1000) NOT NULL,
    lead_id   BIGINT        NOT NULL,
    UNIQUE (sheet_id, name)
);

CREATE TABLE crew_member (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(255) NOT NULL,
    crew_id   BIGINT       NOT NULL,
    person_id BIGINT       NOT NULL,
    UNIQUE (crew_id, person_id)
);

CREATE TABLE crew_hist AS SELECT * FROM crew WITH NO DATA;
ALTER TABLE crew_hist ADD COLUMN hist_seq BIGINT NOT NULL;
ALTER TABLE crew_hist ADD COLUMN hist_made_on DATE NOT NULL;
ALTER TABLE crew_hist ADD COLUMN hist_gone BOOLEAN DEFAULT FALSE NOT NULL;
CREATE INDEX crew_hist_key ON crew_hist (id, hist_seq);
CREATE TRIGGER crew_history AFTER INSERT, UPDATE, DELETE ON crew FOR EACH ROW
    CALL 'net.officefloor.hq.app.history.HistoryTrigger';

CREATE TABLE crew_member_hist AS SELECT * FROM crew_member WITH NO DATA;
ALTER TABLE crew_member_hist ADD COLUMN hist_seq BIGINT NOT NULL;
ALTER TABLE crew_member_hist ADD COLUMN hist_made_on DATE NOT NULL;
ALTER TABLE crew_member_hist ADD COLUMN hist_gone BOOLEAN DEFAULT FALSE NOT NULL;
CREATE INDEX crew_member_hist_key ON crew_member_hist (id, hist_seq);
CREATE TRIGGER crew_member_history AFTER INSERT, UPDATE, DELETE ON crew_member FOR EACH ROW
    CALL 'net.officefloor.hq.app.history.HistoryTrigger';
