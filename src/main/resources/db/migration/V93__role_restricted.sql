-- A fourth role: a restricted user sees on the people list only the people of the crews they lead or
-- belong to, and themselves. The role check is unnamed, so the table is rebuilt with the wider one.
CREATE TABLE sheet_role_new (
    sheet_id   VARCHAR(200) NOT NULL REFERENCES sheet (id),
    user_name  VARCHAR(200) NOT NULL,
    role       VARCHAR(20)  NOT NULL CHECK (role IN ('clerk', 'steward', 'viewer', 'restricted')),
    set_by     VARCHAR(200) NOT NULL,
    set_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (sheet_id, user_name)
);
INSERT INTO sheet_role_new (sheet_id, user_name, role, set_by, set_at)
    SELECT sheet_id, user_name, role, set_by, set_at FROM sheet_role;
DROP TABLE sheet_role;
ALTER TABLE sheet_role_new RENAME TO sheet_role;
