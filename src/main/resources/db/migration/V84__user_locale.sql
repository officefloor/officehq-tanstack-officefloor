-- Each user's locale (how the screen shows them figures), kept by their signed-in name. A user with no row
-- reads 'en'. Not a sheet's state: it has no sheet_id, so it is neither captured nor cloned.
CREATE TABLE user_locale (
    user_name VARCHAR(255) PRIMARY KEY,
    locale VARCHAR(8) NOT NULL
);
