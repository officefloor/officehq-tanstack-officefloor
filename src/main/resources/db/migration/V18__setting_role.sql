-- A sheet's settings, by key; a null value is blank (for clerk-limit: no limit). A key with no row is blank.
CREATE TABLE sheet_setting (
    sheet_id   VARCHAR(200) NOT NULL REFERENCES sheet (id),
    setting    VARCHAR(100) NOT NULL,
    val        VARCHAR(200),
    set_by     VARCHAR(200) NOT NULL,
    set_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (sheet_id, setting)
);
-- The role a user name has on a sheet: clerk or steward. A name with no row is a clerk.
CREATE TABLE sheet_role (
    sheet_id   VARCHAR(200) NOT NULL REFERENCES sheet (id),
    user_name  VARCHAR(200) NOT NULL,
    role       VARCHAR(20)  NOT NULL CHECK (role IN ('clerk', 'steward')),
    set_by     VARCHAR(200) NOT NULL,
    set_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (sheet_id, user_name)
);
