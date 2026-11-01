-- The locale each user has chosen for how the screen shows figures (en or de). Kept per user, not
-- per sheet, and not part of any sheet's data (so no asof copies); a user with no row is en.
CREATE TABLE user_locale (
    user_name VARCHAR(200) NOT NULL PRIMARY KEY,
    locale    VARCHAR(10)  NOT NULL
);
