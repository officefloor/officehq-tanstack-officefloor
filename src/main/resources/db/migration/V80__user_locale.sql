-- Each user's locale (en or de), kept per user across every sheet: how the screen shows them figures. A user with no
-- row has en.
CREATE TABLE user_locale (
    user_name  VARCHAR(200) PRIMARY KEY,
    locale     VARCHAR(10)  NOT NULL
);
