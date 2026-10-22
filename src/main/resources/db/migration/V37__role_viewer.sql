-- A third role, viewer: sees everything on the sheet and may change nothing. The role check is widened by
-- rebuilding the table (its CHECK constraint was unnamed).
CREATE TABLE sheet_role_new (
    sheet_id  VARCHAR(100) NOT NULL REFERENCES sheet(id),
    user_name VARCHAR(100) NOT NULL,
    role      VARCHAR(20)  NOT NULL CHECK (role IN ('clerk', 'steward', 'viewer')),
    PRIMARY KEY (sheet_id, user_name)
);
INSERT INTO sheet_role_new (sheet_id, user_name, role) SELECT sheet_id, user_name, role FROM sheet_role;
DROP TABLE sheet_role;
ALTER TABLE sheet_role_new RENAME TO sheet_role;
