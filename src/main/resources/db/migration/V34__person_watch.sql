-- A user watches a person on a sheet, each user for themselves. A watch's id orders the watchers of a
-- person by when they started watching; stopping deletes it, so watching again starts a new one.
CREATE TABLE person_watch (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id   VARCHAR(200) NOT NULL REFERENCES sheet (id),
    person_id  BIGINT       NOT NULL REFERENCES person (id),
    watched_by VARCHAR(200) NOT NULL,
    watched_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT person_watch_unique UNIQUE (sheet_id, person_id, watched_by)
);
CREATE INDEX person_watch_user ON person_watch (sheet_id, watched_by);
