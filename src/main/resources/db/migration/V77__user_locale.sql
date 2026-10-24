-- A user name's locale: how the screen shows that user's figures (en: 1234.56, de: 1.234,56). Kept per user, across
-- sheets; a name with none is en.
CREATE TABLE user_locale (
    user_name VARCHAR(200) PRIMARY KEY,
    locale VARCHAR(10) NOT NULL
);
