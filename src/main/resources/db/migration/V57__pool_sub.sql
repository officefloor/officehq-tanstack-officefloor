-- Pools that are members of another pool, with a share percent like a person's. A pool is a member of at most one
-- pool (the primary key on sheet and member), never contains itself through any chain, and a chain of pools is at
-- most three deep. Its position is drawn from the same order as pool_member, so a pool's members, people and pools,
-- list in the order added.
CREATE TABLE pool_sub (
    sheet    VARCHAR(200)  NOT NULL,
    pool     VARCHAR(200)  NOT NULL,
    member   VARCHAR(200)  NOT NULL,
    share    DECIMAL(5, 2) NOT NULL,
    position INT           NOT NULL,
    PRIMARY KEY (sheet, member)
);
