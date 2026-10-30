-- Each user's locale, kept per user (across every sheet). A user with no row reads as 'en'.
CREATE TABLE user_locale (
    user_name VARCHAR(200) NOT NULL PRIMARY KEY,
    locale    VARCHAR(10)  NOT NULL
);
