-- Each lock and unlock of a period, in the order made, dated the sheet's today when made (set_on), so the as-of view
-- shows a period locked as it stood then. A NULL set_on is a lock carried over from before this history was kept: it
-- counts as of any date.
CREATE TABLE period_lock_history (
    seq      BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL,
    name     VARCHAR(200) NOT NULL,
    set_on   DATE,
    locked   BOOLEAN      NOT NULL
);
INSERT INTO period_lock_history (sheet_id, name, set_on, locked) SELECT sheet_id, name, NULL, TRUE FROM period WHERE locked;
