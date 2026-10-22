-- A pool member may be another pool (member_pool_id; person_id then null), with a share like a person's. A pool is a
-- member of at most one pool.
ALTER TABLE pool_member ALTER COLUMN person_id DROP NOT NULL;
ALTER TABLE pool_member ADD COLUMN member_pool_id BIGINT REFERENCES pool(id);
ALTER TABLE pool_member ADD CONSTRAINT pool_member_pool UNIQUE (member_pool_id);
