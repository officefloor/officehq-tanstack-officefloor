-- The people each user watches, per sheet: a user watches a person at most once. The id keeps the
-- order they started watching, which is the order a settled claim's watchers are notified in.
CREATE TABLE person_watch (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet(id),
    person_id BIGINT NOT NULL REFERENCES person(id),
    user_name VARCHAR(200) NOT NULL,
    UNIQUE (person_id, user_name)
);
CREATE INDEX person_watch_sheet ON person_watch(sheet_id, person_id, id);
