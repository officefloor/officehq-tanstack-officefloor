-- The people each user watches on a sheet, in the order they started watching (seq). Stopping removes
-- the row, so watching again starts afresh at the end.
CREATE TABLE person_watch (
    seq       BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200) NOT NULL,
    person_id BIGINT       NOT NULL,
    user_name VARCHAR(200) NOT NULL,
    UNIQUE (sheet_id, person_id, user_name)
);
