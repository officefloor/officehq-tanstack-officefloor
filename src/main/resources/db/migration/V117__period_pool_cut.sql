-- Each pool member's cut in a closed or soft-closed period, fixed as it stood (as the pools show it on the sheet's
-- today) at the moment the period was closed: a person (person_id) or a member pool (member_pool_id) in a pool (a
-- crew's pool by its negated crew id); cut NULL when the member had none then. Dropped when the period is reopened.
CREATE TABLE period_pool_cut (
    sheet_id       VARCHAR(100)   NOT NULL REFERENCES sheet(id),
    period_name    VARCHAR(200)   NOT NULL,
    pool_id        BIGINT         NOT NULL,
    person_id      BIGINT,
    member_pool_id BIGINT,
    cut            DECIMAL(14, 2)
);
CREATE INDEX period_pool_cut_period ON period_pool_cut (sheet_id, period_name);
