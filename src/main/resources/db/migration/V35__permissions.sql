-- The sheet's permission matrix: for an action (open, settle, void, reassign, close, reopen) the roles
-- that may take it, comma separated. No row (or a blank list): the standing rule — open, settle, void
-- and close by clerk and steward; reassign and reopen by steward. Not dated.
CREATE TABLE sheet_permission (
    sheet_id VARCHAR(200) NOT NULL,
    action   VARCHAR(20)  NOT NULL,
    roles    VARCHAR(200) NOT NULL,
    PRIMARY KEY (sheet_id, action)
);
