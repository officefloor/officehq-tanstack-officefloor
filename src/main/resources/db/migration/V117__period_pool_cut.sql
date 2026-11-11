-- Each pool member's cut for a closed (or soft-closed) period, fixed when the period was closed: as
-- it stood at that moment, with two decimals ('' for none). The member is the person's id, or
-- pool-<name> for a member pool. Removed when the period is reopened, making the cuts live again.
CREATE TABLE period_pool_cut (
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet(id),
    period_name VARCHAR(200) NOT NULL,
    pool VARCHAR(200) NOT NULL,
    member_key VARCHAR(200) NOT NULL,
    cut VARCHAR(30) NOT NULL,
    PRIMARY KEY (sheet_id, period_name, pool, member_key)
);
