-- A user watching a person, kept per sheet; one row per watching user, in the order they started
-- watching (a user who stops and starts again goes to the end).
CREATE TABLE person_watch (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200) NOT NULL,
    person_id BIGINT       NOT NULL REFERENCES person (id),
    user_name VARCHAR(200) NOT NULL
);
CREATE INDEX person_watch_sheet ON person_watch (sheet_id, person_id, id);
