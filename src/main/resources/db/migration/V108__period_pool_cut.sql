-- Each pool member's cut for a closed or soft-closed period, fixed as it stood when the period was closed
-- (null for a member with no cut then), by pool name and member key (a person's id, or pool:<name>).
-- Dropped when the period is reopened.
CREATE TABLE period_pool_cut (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    period_id  BIGINT         NOT NULL REFERENCES period (id),
    sheet_id   VARCHAR(200)   NOT NULL REFERENCES sheet (id),
    pool_name  VARCHAR(500)   NOT NULL,
    member_key VARCHAR(600)   NOT NULL,
    cut        DECIMAL(19, 2)
);
