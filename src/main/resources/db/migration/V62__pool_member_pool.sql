-- A pool's member may instead be another pool of the sheet (member_pool_id set, person_id null), with a share like a
-- person's. A pool is a member of at most one pool, never of itself through any chain, and a chain of pools is at
-- most three deep.
ALTER TABLE pool_member ALTER COLUMN person_id SET NULL;
ALTER TABLE pool_member ADD COLUMN member_pool_id BIGINT REFERENCES pool (id);
CREATE UNIQUE INDEX pool_member_member_pool ON pool_member (member_pool_id);
