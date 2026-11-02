-- The people a user watches, per sheet. A watcher is told (a NOTIFY audit line) when one of the person's
-- claims is settled, approved or re-settled; watchers are told in the order they started watching (by id).
CREATE TABLE person_watch (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200) NOT NULL REFERENCES sheet (id),
    person_id BIGINT       NOT NULL REFERENCES person (id),
    user_name VARCHAR(200) NOT NULL,
    UNIQUE (person_id, user_name)
);
CREATE INDEX person_watch_sheet ON person_watch (sheet_id, user_name);
