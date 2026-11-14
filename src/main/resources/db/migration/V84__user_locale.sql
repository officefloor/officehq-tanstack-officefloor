-- The locale a signed-in user has the sheet's figures shown in ('en' or 'de'); no row is 'en'.
CREATE TABLE user_locale (
    sheet_id VARCHAR(100) NOT NULL REFERENCES sheet (id),
    user_name VARCHAR(100) NOT NULL,
    locale VARCHAR(10) NOT NULL,
    PRIMARY KEY (sheet_id, user_name)
);
