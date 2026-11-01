-- The people each user watches on the sheet, each at most once; the id keeps the order they started watching.
CREATE TABLE watch (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet     VARCHAR(200) NOT NULL,
    person_id BIGINT       NOT NULL,
    user_name VARCHAR(200) NOT NULL,
    UNIQUE (sheet, person_id, user_name)
);
