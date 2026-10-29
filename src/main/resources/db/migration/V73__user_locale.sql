-- Each signed-in user's locale (how figures are shown to them), kept per user across every sheet. A user with no row
-- is shown en.
CREATE TABLE user_locale (
    user_name VARCHAR(255) PRIMARY KEY,
    locale    VARCHAR(8)   NOT NULL
);
