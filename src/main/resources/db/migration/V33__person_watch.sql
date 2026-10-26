-- Who watches whom: one row per signed-in user watching a person, in the order they started watching
-- (id). Unwatching deletes the row, so watching again starts afresh at the end.
CREATE TABLE person_watch (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    person_id BIGINT       NOT NULL REFERENCES person (id),
    user_name VARCHAR(255) NOT NULL
);
CREATE INDEX person_watch_person ON person_watch (person_id, id);
