-- The sheet's pools: a name (trimmed) at most once on a sheet, and the day it was added (the sheet's today then).
CREATE TABLE pool (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(100) NOT NULL REFERENCES sheet(id),
    name     VARCHAR(200) NOT NULL,
    added_by VARCHAR(100) NOT NULL,
    added_on DATE,
    CONSTRAINT pool_name UNIQUE (sheet_id, name)
);

-- A pool's members: a person at most once in a pool, with their share percent (more than 0.00, at most 100.00).
CREATE TABLE pool_member (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(100)  NOT NULL REFERENCES sheet(id),
    pool_id   BIGINT        NOT NULL REFERENCES pool(id),
    person_id BIGINT        NOT NULL,
    share     DECIMAL(5, 2) NOT NULL,
    added_by  VARCHAR(100)  NOT NULL,
    added_on  DATE,
    CONSTRAINT pool_member_person UNIQUE (pool_id, person_id)
);
