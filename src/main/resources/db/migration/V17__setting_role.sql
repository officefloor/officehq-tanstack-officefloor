-- A sheet's settings, by key: the clerk limit (clerk-limit, two decimals; none: no limit).
CREATE TABLE setting (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id      VARCHAR(100) NOT NULL,
    setting_key   VARCHAR(50)  NOT NULL,
    setting_value VARCHAR(100)
);
CREATE UNIQUE INDEX setting_sheet_key ON setting (sheet_id, setting_key);

-- The role a user name has on a sheet: clerk or steward. A name with none is a clerk.
-- A clerk can settle only a claim whose due is at or below the clerk limit; a steward settles any.
CREATE TABLE user_role (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(100) NOT NULL,
    user_name VARCHAR(100) NOT NULL,
    role      VARCHAR(20)  NOT NULL
);
CREATE UNIQUE INDEX user_role_sheet_user ON user_role (sheet_id, user_name);
