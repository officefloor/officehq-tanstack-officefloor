-- The as-of view shows every cell as it stood at the end of a date (see V39__history.sql), so the rest
-- of what the screen shows is kept by version too: statements, periods and their fixed figures, patrons,
-- watches, roles and delegations. A sheet records the sheet's today it was archived on.
CREATE TABLE statement_hist AS SELECT * FROM statement WITH NO DATA;
CREATE TABLE period_hist AS SELECT * FROM period WITH NO DATA;
CREATE TABLE period_person_total_hist AS SELECT * FROM period_person_total WITH NO DATA;
CREATE TABLE patron_hist AS SELECT * FROM patron WITH NO DATA;
CREATE TABLE watch_hist AS SELECT * FROM watch WITH NO DATA;
CREATE TABLE sheet_role_hist AS SELECT * FROM sheet_role WITH NO DATA;
CREATE TABLE delegation_hist AS SELECT * FROM delegation WITH NO DATA;

ALTER TABLE statement_hist ADD COLUMN hist_seq BIGINT NOT NULL;
ALTER TABLE statement_hist ADD COLUMN hist_made_on DATE NOT NULL;
ALTER TABLE statement_hist ADD COLUMN hist_gone BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE period_hist ADD COLUMN hist_seq BIGINT NOT NULL;
ALTER TABLE period_hist ADD COLUMN hist_made_on DATE NOT NULL;
ALTER TABLE period_hist ADD COLUMN hist_gone BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE period_person_total_hist ADD COLUMN hist_seq BIGINT NOT NULL;
ALTER TABLE period_person_total_hist ADD COLUMN hist_made_on DATE NOT NULL;
ALTER TABLE period_person_total_hist ADD COLUMN hist_gone BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE patron_hist ADD COLUMN hist_seq BIGINT NOT NULL;
ALTER TABLE patron_hist ADD COLUMN hist_made_on DATE NOT NULL;
ALTER TABLE patron_hist ADD COLUMN hist_gone BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE watch_hist ADD COLUMN hist_seq BIGINT NOT NULL;
ALTER TABLE watch_hist ADD COLUMN hist_made_on DATE NOT NULL;
ALTER TABLE watch_hist ADD COLUMN hist_gone BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE sheet_role_hist ADD COLUMN hist_seq BIGINT NOT NULL;
ALTER TABLE sheet_role_hist ADD COLUMN hist_made_on DATE NOT NULL;
ALTER TABLE sheet_role_hist ADD COLUMN hist_gone BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE delegation_hist ADD COLUMN hist_seq BIGINT NOT NULL;
ALTER TABLE delegation_hist ADD COLUMN hist_made_on DATE NOT NULL;
ALTER TABLE delegation_hist ADD COLUMN hist_gone BOOLEAN DEFAULT FALSE NOT NULL;

CREATE INDEX statement_hist_key ON statement_hist (id, hist_seq);
CREATE INDEX period_hist_key ON period_hist (id, hist_seq);
CREATE INDEX period_person_total_hist_key ON period_person_total_hist (sheet_id, period_name, person_id, hist_seq);
CREATE INDEX patron_hist_key ON patron_hist (sheet_id, person_id, patron_id, hist_seq);
CREATE INDEX watch_hist_key ON watch_hist (id, hist_seq);
CREATE INDEX sheet_role_hist_key ON sheet_role_hist (sheet_id, user_name, hist_seq);
CREATE INDEX delegation_hist_key ON delegation_hist (id, hist_seq);

CREATE TRIGGER statement_history AFTER INSERT, UPDATE, DELETE ON statement FOR EACH ROW
    CALL 'net.officefloor.hq.app.history.HistoryTrigger';
CREATE TRIGGER period_history AFTER INSERT, UPDATE, DELETE ON period FOR EACH ROW
    CALL 'net.officefloor.hq.app.history.HistoryTrigger';
CREATE TRIGGER period_person_total_history AFTER INSERT, UPDATE, DELETE ON period_person_total FOR EACH ROW
    CALL 'net.officefloor.hq.app.history.HistoryTrigger';
CREATE TRIGGER patron_history AFTER INSERT, UPDATE, DELETE ON patron FOR EACH ROW
    CALL 'net.officefloor.hq.app.history.HistoryTrigger';
CREATE TRIGGER watch_history AFTER INSERT, UPDATE, DELETE ON watch FOR EACH ROW
    CALL 'net.officefloor.hq.app.history.HistoryTrigger';
CREATE TRIGGER sheet_role_history AFTER INSERT, UPDATE, DELETE ON sheet_role FOR EACH ROW
    CALL 'net.officefloor.hq.app.history.HistoryTrigger';
CREATE TRIGGER delegation_history AFTER INSERT, UPDATE, DELETE ON delegation FOR EACH ROW
    CALL 'net.officefloor.hq.app.history.HistoryTrigger';

ALTER TABLE sheet ADD COLUMN archived_on DATE;
