-- The sheet's permission matrix: for an action (open, settle, void, reassign, close, reopen) the roles that may take
-- it, a comma separated list of clerk and steward. Not dated. An action with no row (or a blank one) keeps the
-- standing rule: open, settle, void and close by clerk and steward; reassign and reopen by steward.
CREATE TABLE sheet_permission (
    sheet_id VARCHAR(100) NOT NULL REFERENCES sheet(id),
    action   VARCHAR(20)  NOT NULL,
    roles    VARCHAR(100) NOT NULL,
    PRIMARY KEY (sheet_id, action)
);
