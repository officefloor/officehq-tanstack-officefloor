-- Each pool member's cut fixed when a period is closed or soft-closed, as it stood then (null when the member had
-- none): the pool (a crew's the negated crew id), and the person or the member pool. Dropped when it is reopened.
CREATE TABLE period_pool_cut (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id       VARCHAR(255)   NOT NULL,
    period_name    VARCHAR(1000)  NOT NULL,
    pool_id        BIGINT         NOT NULL,
    person_id      BIGINT,
    member_pool_id BIGINT,
    cut            DECIMAL(19, 2)
);
CREATE INDEX period_pool_cut_period ON period_pool_cut (sheet_id, period_name);
