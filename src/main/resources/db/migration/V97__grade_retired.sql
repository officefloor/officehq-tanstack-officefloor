-- The grades retired on a sheet: a retired grade cannot be chosen for a new person or in an edit, while
-- the people already on it keep it. Each grade at most once per sheet. Kept by version like the rest of the screen.
CREATE TABLE grade_retired (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(255) NOT NULL,
    grade    VARCHAR(16)  NOT NULL,
    UNIQUE (sheet_id, grade)
);

CREATE TABLE grade_retired_hist AS SELECT * FROM grade_retired WITH NO DATA;
ALTER TABLE grade_retired_hist ADD COLUMN hist_seq BIGINT NOT NULL;
ALTER TABLE grade_retired_hist ADD COLUMN hist_made_on DATE NOT NULL;
ALTER TABLE grade_retired_hist ADD COLUMN hist_gone BOOLEAN DEFAULT FALSE NOT NULL;
CREATE INDEX grade_retired_hist_key ON grade_retired_hist (id, hist_seq);
CREATE TRIGGER grade_retired_history AFTER INSERT, UPDATE, DELETE ON grade_retired FOR EACH ROW
    CALL 'net.officefloor.hq.app.history.HistoryTrigger';
