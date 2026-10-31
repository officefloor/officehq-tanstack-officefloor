-- A sheet's permission matrix: for each action (open, settle, void, reassign, close, reopen) the
-- roles that may take it, a comma separated list (clerk, steward). Not dated. No row (or a null
-- roles) keeps the standing rule: open, settle, void and close by clerk and steward; reassign and
-- reopen by steward.
CREATE TABLE sheet_permission (
    sheet_id   VARCHAR(200) NOT NULL,
    action_key VARCHAR(20)  NOT NULL,
    roles      VARCHAR(100),
    PRIMARY KEY (sheet_id, action_key)
);
