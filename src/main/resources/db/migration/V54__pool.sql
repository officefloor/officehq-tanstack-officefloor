-- The sheet's pools, each name (trimmed) unique on it, in the order added, and their members: people on the sheet,
-- each at most once in a pool with a share percent (two decimals, above 0.00 and at most 100.00). A person can be in
-- several pools.
CREATE TABLE pool (
    sheet    VARCHAR(200) NOT NULL,
    name     VARCHAR(200) NOT NULL,
    position INT          NOT NULL,
    PRIMARY KEY (sheet, name)
);
CREATE TABLE pool_member (
    sheet     VARCHAR(200)  NOT NULL,
    pool      VARCHAR(200)  NOT NULL,
    person_id BIGINT        NOT NULL,
    share     DECIMAL(5, 2) NOT NULL,
    position  INT           NOT NULL,
    PRIMARY KEY (sheet, pool, person_id)
);
