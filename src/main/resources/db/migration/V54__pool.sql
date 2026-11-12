-- The sheet's pools: a name (trimmed, at most once per sheet) and its members, each a person on the sheet
-- (at most once per pool) with a share percent (two decimals, more than 0 and at most 100). A person may be
-- in several pools. Kept by version like the rest of the screen.
CREATE TABLE pool (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(255)  NOT NULL,
    name     VARCHAR(1000) NOT NULL,
    UNIQUE (sheet_id, name)
);

CREATE TABLE pool_member (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(255)  NOT NULL,
    pool_id   BIGINT        NOT NULL,
    person_id BIGINT        NOT NULL,
    share     DECIMAL(5, 2) NOT NULL,
    UNIQUE (pool_id, person_id)
);

CREATE TABLE pool_hist AS SELECT * FROM pool WITH NO DATA;
ALTER TABLE pool_hist ADD COLUMN hist_seq BIGINT NOT NULL;
ALTER TABLE pool_hist ADD COLUMN hist_made_on DATE NOT NULL;
ALTER TABLE pool_hist ADD COLUMN hist_gone BOOLEAN DEFAULT FALSE NOT NULL;
CREATE INDEX pool_hist_key ON pool_hist (id, hist_seq);
CREATE TRIGGER pool_history AFTER INSERT, UPDATE, DELETE ON pool FOR EACH ROW
    CALL 'net.officefloor.hq.app.history.HistoryTrigger';

CREATE TABLE pool_member_hist AS SELECT * FROM pool_member WITH NO DATA;
ALTER TABLE pool_member_hist ADD COLUMN hist_seq BIGINT NOT NULL;
ALTER TABLE pool_member_hist ADD COLUMN hist_made_on DATE NOT NULL;
ALTER TABLE pool_member_hist ADD COLUMN hist_gone BOOLEAN DEFAULT FALSE NOT NULL;
CREATE INDEX pool_member_hist_key ON pool_member_hist (id, hist_seq);
CREATE TRIGGER pool_member_history AFTER INSERT, UPDATE, DELETE ON pool_member FOR EACH ROW
    CALL 'net.officefloor.hq.app.history.HistoryTrigger';
