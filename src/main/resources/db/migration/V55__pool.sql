-- The sheet's pools: each name (trimmed, not blank) at most once on the sheet, kept in the order added.
CREATE TABLE pool (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL,
    name     VARCHAR(500) NOT NULL,
    CONSTRAINT pool_unique UNIQUE (sheet_id, name)
);

-- A pool's members: a person at most once in a pool, with a share percent (more than 0.00, at most 100.00). A person
-- can be in several pools. A retired member's share counts as 0.00 toward the pool's total.
CREATE TABLE pool_member (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    pool_id   BIGINT        NOT NULL REFERENCES pool (id),
    person_id BIGINT        NOT NULL REFERENCES person (id),
    share     DECIMAL(5, 2) NOT NULL,
    CONSTRAINT pool_member_unique UNIQUE (pool_id, person_id)
);
