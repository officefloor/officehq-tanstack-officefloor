-- Each pool member's cut fixed when a period was closed (or soft-closed), by the member of the pool it is
-- (<pool id>:person:<id> or <pool id>:pool:<id>), '' for one with no cut; the rows go when it is reopened.
CREATE TABLE period_pool_cut (
    period_id BIGINT NOT NULL REFERENCES period (id),
    member_key VARCHAR(255) NOT NULL,
    cut VARCHAR(32) NOT NULL,
    PRIMARY KEY (period_id, member_key)
);
