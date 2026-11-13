-- The sheet's pools: a name (trimmed, unique on the sheet), listed in the order they were added (id order).
CREATE TABLE pool (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(100) NOT NULL REFERENCES sheet (id),
    name VARCHAR(200) NOT NULL,
    UNIQUE (sheet_id, name)
);
-- A pool's members: a person (at most once per pool, in any number of pools) with a share percent (more than 0.00,
-- at most 100.00), listed in the order they were added (id order).
CREATE TABLE pool_member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pool_id BIGINT NOT NULL REFERENCES pool (id),
    person_id BIGINT NOT NULL REFERENCES person (id),
    share DECIMAL(5, 2) NOT NULL,
    UNIQUE (pool_id, person_id)
);
