-- The locale each user has chosen on a sheet ('en' or 'de'); a user without a row is shown 'en'. Only how
-- the screen shows figures follows it, so it is not kept in history.
CREATE TABLE user_locale (
    sheet_id VARCHAR(255) NOT NULL,
    username VARCHAR(255) NOT NULL,
    locale   VARCHAR(8)   NOT NULL,
    PRIMARY KEY (sheet_id, username)
);
