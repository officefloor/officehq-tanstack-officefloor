-- The sheet's pools: a name (stored trimmed, at most once on its sheet: the app refuses a repeat with a message, the
-- index is the backstop) and its members, each a person of the sheet at most once in the pool with a share percent
-- (two decimals, more than 0.00 and at most 100.00). A person can be in several pools.
CREATE TABLE pool (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet (id),
    name     VARCHAR(200) NOT NULL
);
CREATE UNIQUE INDEX pool_sheet_name ON pool (sheet_id, name);

CREATE TABLE pool_member (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200) NOT NULL REFERENCES sheet (id),
    pool_id   BIGINT       NOT NULL REFERENCES pool (id),
    person_id BIGINT       NOT NULL REFERENCES person (id),
    share     DECIMAL(5, 2) NOT NULL
);
CREATE UNIQUE INDEX pool_member_pool_person ON pool_member (pool_id, person_id);

-- Snapshotted with the rest of the screen (see V40, V41).
CREATE TABLE hist.pool AS SELECT t.*, CAST(NULL AS BIGINT) AS snapshot_id FROM pool t WHERE FALSE;
CREATE INDEX hist_pool_snapshot ON hist.pool (snapshot_id);
CREATE TABLE asof.pool AS SELECT * FROM pool WHERE FALSE;
CREATE TABLE hist.pool_member AS SELECT t.*, CAST(NULL AS BIGINT) AS snapshot_id FROM pool_member t WHERE FALSE;
CREATE INDEX hist_pool_member_snapshot ON hist.pool_member (snapshot_id);
CREATE TABLE asof.pool_member AS SELECT * FROM pool_member WHERE FALSE;
