-- A sheet's pools: a trimmed name, at most once per sheet. A pool's members are people on the sheet,
-- each at most once per pool, with a share percent (two decimals, more than 0.00, at most 100.00).
-- A person can be in several pools.
CREATE TABLE pool (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet(id),
    name VARCHAR(200) NOT NULL,
    UNIQUE (sheet_id, name)
);
CREATE TABLE pool_member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pool_id BIGINT NOT NULL REFERENCES pool(id),
    person_id BIGINT NOT NULL REFERENCES person(id),
    share DECIMAL(5, 2) NOT NULL,
    UNIQUE (pool_id, person_id)
);
