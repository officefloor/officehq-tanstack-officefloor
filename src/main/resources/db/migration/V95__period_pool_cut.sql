-- A closed or soft-closed period's pool cuts: every pool member's cut (crews' pools too) as it stood when
-- the period was closed, keyed by the member's place (the pool, and the person or pool that is the member;
-- cut "" when they had none). Shown for the period from then on; removed when the period is reopened.
CREATE TABLE period_pool_cut (
    period_id  BIGINT       NOT NULL REFERENCES period (id),
    member_key VARCHAR(100) NOT NULL,
    cut        VARCHAR(30)  NOT NULL,
    PRIMARY KEY (period_id, member_key)
);
