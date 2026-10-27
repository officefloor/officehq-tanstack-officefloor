-- The sheet's pools: a name (trimmed, at most once per sheet), listed in the order added.
CREATE TABLE pool (
    sheet_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    listed BIGINT NOT NULL,
    PRIMARY KEY (sheet_id, name)
);
-- A pool's members: a person of the sheet, at most once per pool, with a share percent (two decimals, above 0.00 and
-- at most 100.00). A person can be in several pools. A retired member's share counts 0.00 toward the pool's total.
CREATE TABLE pool_member (
    sheet_id VARCHAR(255) NOT NULL,
    pool_name VARCHAR(255) NOT NULL,
    person_id BIGINT NOT NULL,
    share DECIMAL(5, 2) NOT NULL,
    listed BIGINT NOT NULL,
    PRIMARY KEY (sheet_id, pool_name, person_id)
);
