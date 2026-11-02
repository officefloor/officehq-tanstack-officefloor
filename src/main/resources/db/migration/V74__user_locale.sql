-- The locale a user name chose on the sheet: en or de. A name with no row uses en.
CREATE TABLE user_locale (
    sheet     VARCHAR(200) NOT NULL,
    user_name VARCHAR(200) NOT NULL,
    locale    VARCHAR(10)  NOT NULL,
    PRIMARY KEY (sheet, user_name)
);
