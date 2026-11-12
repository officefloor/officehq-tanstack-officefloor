-- Who watches whom: a user watching a person on the sheet, each at most once. The id orders a person's
-- watchers by when they started watching; watching again after stopping starts afresh.
CREATE TABLE watch (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(255) NOT NULL,
    person_id BIGINT       NOT NULL,
    username  VARCHAR(255) NOT NULL,
    UNIQUE (sheet_id, person_id, username)
);
