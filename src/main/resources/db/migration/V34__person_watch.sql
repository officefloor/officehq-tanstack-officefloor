-- The users watching each person, in the order they started watching (id order). A user watches a person at most once.
CREATE TABLE person_watch (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    person_id BIGINT NOT NULL REFERENCES person (id),
    user_name VARCHAR(200) NOT NULL,
    CONSTRAINT person_watch_unique UNIQUE (person_id, user_name)
);
