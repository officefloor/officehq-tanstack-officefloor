-- Each user's locale, for themselves across every sheet: how the screen shows them figures. A user
-- with no row is en.
CREATE TABLE user_locale (
    user_name VARCHAR(200) PRIMARY KEY,
    locale    VARCHAR(10)  NOT NULL
);
