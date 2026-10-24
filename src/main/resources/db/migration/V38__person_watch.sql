-- Who watches a person: one row per signed-in name watching them, in the order they started watching.
-- Stopping watching removes the row; starting again adds a new one at the end.
CREATE TABLE person_watch (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet (id),
    person_id BIGINT NOT NULL REFERENCES person (id),
    user_name VARCHAR(200) NOT NULL,
    CONSTRAINT person_watch_unique UNIQUE (person_id, user_name)
);
CREATE INDEX person_watch_sheet ON person_watch (sheet_id, user_name);
