-- A sheet's pools: a name (unique on the sheet, trimmed, not blank).
CREATE TABLE pool (
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet (id),
    name     VARCHAR(500) NOT NULL,
    PRIMARY KEY (sheet_id, name)
);
-- A pool's members: a person on the sheet (at most once per pool) with a share percent (more than 0.00,
-- at most 100.00, two decimals), listed in the order they were added (by id). A retired member's share
-- counts as 0.00 toward the pool's total.
CREATE TABLE pool_member (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200)  NOT NULL REFERENCES sheet (id),
    pool_name VARCHAR(500)  NOT NULL,
    person_id BIGINT        NOT NULL REFERENCES person (id),
    share     DECIMAL(5, 2) NOT NULL CHECK (share > 0 AND share <= 100),
    UNIQUE (sheet_id, pool_name, person_id)
);
