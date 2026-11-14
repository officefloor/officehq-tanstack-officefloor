-- Each pool member's cut for a closed or soft-closed period, fixed as it stood when the period was closed (cut: '' for
-- none): a person member (person_id) or a pool member (member_pool_id) of the pool (pool_id; a crew's pool has the
-- crew's id negated). A period with rows here shows them as its cuts; cleared when the period reopens.
CREATE TABLE period_pool_cut (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(100) NOT NULL REFERENCES sheet (id),
    period_name VARCHAR(100) NOT NULL,
    pool_id BIGINT NOT NULL,
    person_id BIGINT,
    member_pool_id BIGINT,
    cut VARCHAR(30) NOT NULL
);
CREATE INDEX period_pool_cut_sheet ON period_pool_cut (sheet_id, period_name);
