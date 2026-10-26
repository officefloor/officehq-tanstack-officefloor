-- A third role, viewer: sees everything and may change nothing. The role check is rebuilt to allow it.
CREATE TABLE sheet_role_new (
    sheet_id  VARCHAR(255) NOT NULL,
    user_name VARCHAR(255) NOT NULL,
    role      VARCHAR(16)  NOT NULL CHECK (role IN ('clerk', 'steward', 'viewer')),
    PRIMARY KEY (sheet_id, user_name)
);
INSERT INTO sheet_role_new (sheet_id, user_name, role) SELECT sheet_id, user_name, role FROM sheet_role;
DROP TABLE sheet_role;
ALTER TABLE sheet_role_new RENAME TO sheet_role;
