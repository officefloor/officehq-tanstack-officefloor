-- Each user's locale (en or de), how the screen shows that user figures. Kept per user name, across sheets; a user
-- without a row is on en.
CREATE TABLE user_locale (
    user_name VARCHAR(200) PRIMARY KEY,
    locale    VARCHAR(10)  NOT NULL
);
