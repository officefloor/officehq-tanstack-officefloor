-- A rate is kept as versions: setting a rate for a grade and from-date that already has one keeps the value it
-- replaces in rate_version and counts up the rate's version. The row in rate is always the newest version (the one in
-- force); version is how many versions it has had. Kept in the rate's history too (see V39). The journal trigger is
-- made again around the change, as in V65.
DROP TRIGGER rate_journal;
ALTER TABLE rate ADD COLUMN version INT DEFAULT 1 NOT NULL;
ALTER TABLE rate_history ADD COLUMN version INT DEFAULT 1 NOT NULL;
CREATE TRIGGER rate_journal AFTER INSERT, UPDATE, DELETE ON rate FOR EACH ROW CALL 'net.officefloor.hq.app.history.Journal';
CREATE TABLE rate_version (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(255)  NOT NULL,
    grade     VARCHAR(1)    NOT NULL,
    from_date DATE,
    version   INT           NOT NULL,
    rate      DECIMAL(9, 2) NOT NULL,
    to_date   DATE
);
CREATE INDEX rate_version_key ON rate_version (sheet_id, grade, from_date);
