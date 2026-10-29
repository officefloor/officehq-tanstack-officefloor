-- A pool's members that are other pools: a pool is a member of at most one pool, with a share percent like a person's
-- (two decimals, above 0.00 and at most 100.00). A pool never contains itself through any chain, and a chain of pools is
-- at most three deep. Listed in one order with the pool's person members (the next listed of both tables).
CREATE TABLE pool_nested (
    sheet_id VARCHAR(255) NOT NULL,
    pool_name VARCHAR(255) NOT NULL,
    member_pool VARCHAR(255) NOT NULL,
    share DECIMAL(5, 2) NOT NULL,
    listed BIGINT NOT NULL,
    PRIMARY KEY (sheet_id, member_pool)
);
