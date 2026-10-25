-- Each sheet assigns a role to a user name: clerk or steward. A name without a row is a clerk.
CREATE TABLE sheet_role (
    sheet_id  VARCHAR(200) NOT NULL REFERENCES sheet (id),
    user_name VARCHAR(200) NOT NULL,
    role      VARCHAR(20)  NOT NULL CHECK (role IN ('clerk', 'steward')),
    PRIMARY KEY (sheet_id, user_name)
);

-- Each sheet's settings, by key. A null value is blank (for clerk-limit: no limit). A key without a row is blank.
CREATE TABLE sheet_setting (
    sheet_id      VARCHAR(200) NOT NULL REFERENCES sheet (id),
    setting_key   VARCHAR(50)  NOT NULL,
    setting_value VARCHAR(200),
    PRIMARY KEY (sheet_id, setting_key)
);
