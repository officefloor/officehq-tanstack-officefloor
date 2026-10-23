-- Each user's locale (how figures are shown to them), kept per user across every sheet. A user with no row is 'en'.
CREATE TABLE user_locale (
    user_name VARCHAR(100) PRIMARY KEY,
    locale    VARCHAR(10)  NOT NULL
);
