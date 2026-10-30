-- The sheet's pools, by name, in the order added.
CREATE TABLE pool (
    seq      BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200)  NOT NULL,
    name     VARCHAR(2000) NOT NULL,
    UNIQUE (sheet_id, name)
);

-- A pool's members: a person of the sheet with a share percent (more than 0.00, at most 100.00), in
-- the order added. A person is in a pool at most once, but may be in several pools.
CREATE TABLE pool_member (
    seq       BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200)  NOT NULL,
    pool_name VARCHAR(2000) NOT NULL,
    person_id BIGINT        NOT NULL,
    share     DECIMAL(5, 2) NOT NULL,
    UNIQUE (sheet_id, pool_name, person_id)
);
