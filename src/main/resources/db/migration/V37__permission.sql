-- The roles allowed an action (open, settle, void, reassign, close, reopen) on a sheet, comma separated; an action
-- with no row keeps the standing rule (open, settle, void and close by clerk and steward; reassign and reopen by steward).
CREATE TABLE permission (
    sheet_id VARCHAR(100) NOT NULL REFERENCES sheet (id),
    action VARCHAR(20) NOT NULL,
    roles VARCHAR(100) NOT NULL,
    PRIMARY KEY (sheet_id, action)
);
