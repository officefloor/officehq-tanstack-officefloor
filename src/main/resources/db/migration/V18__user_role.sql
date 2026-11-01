-- The role a user name has on the sheet: clerk or steward. A name with no row is a clerk.
CREATE TABLE user_role (
    sheet     VARCHAR(200) NOT NULL,
    user_name VARCHAR(200) NOT NULL,
    role      VARCHAR(20)  NOT NULL,
    PRIMARY KEY (sheet, user_name)
);
