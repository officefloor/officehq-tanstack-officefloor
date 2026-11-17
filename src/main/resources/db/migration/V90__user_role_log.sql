-- Every role given on a sheet, in the order given, with the sheet's today when it was given, so the role a user had
-- as of a date can be read. A user with a role but no line here had it from the beginning.
CREATE TABLE user_role_log (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200) NOT NULL,
    user_name VARCHAR(200) NOT NULL,
    role      VARCHAR(20)  NOT NULL,
    set_on    DATE         NOT NULL
);
CREATE INDEX user_role_log_sheet ON user_role_log (sheet_id, user_name, id);
