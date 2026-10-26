-- A sheet's settings, one value per key (clerk-limit: an amount with two decimals). A key with no
-- row, or a blank value, is not set (a clerk limit not set is no limit).
CREATE TABLE setting (
    sheet_id      VARCHAR(255) NOT NULL,
    setting_key   VARCHAR(64)  NOT NULL,
    setting_value VARCHAR(255) NOT NULL,
    PRIMARY KEY (sheet_id, setting_key)
);
-- The role a sheet gives a user name: clerk or steward. A name with no row is a clerk.
CREATE TABLE sheet_role (
    sheet_id  VARCHAR(255) NOT NULL,
    user_name VARCHAR(255) NOT NULL,
    role      VARCHAR(16)  NOT NULL CHECK (role IN ('clerk', 'steward')),
    PRIMARY KEY (sheet_id, user_name)
);
