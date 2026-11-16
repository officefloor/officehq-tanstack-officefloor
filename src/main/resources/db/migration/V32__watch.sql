-- The people each user watches, per sheet: a user watches a person at most once. The id keeps the order they started
-- watching, so a watched person's settled claim notifies the watchers in that order.
CREATE TABLE watch (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id   VARCHAR(200) NOT NULL,
    person_id  BIGINT       NOT NULL REFERENCES person (id),
    user_name  VARCHAR(200) NOT NULL,
    CONSTRAINT watch_unique UNIQUE (sheet_id, person_id, user_name)
);
