-- A pool's member may instead be another pool of the sheet (member_pool, person_id then null), with a share
-- like a person's. A pool is a member of at most one pool, never of itself through any chain, and a chain of
-- pools is at most three deep. A pool that is a member of another shares out its cut in that pool, not the
-- sheet's pot.
ALTER TABLE pool_member ALTER COLUMN person_id SET NULL;
ALTER TABLE pool_member ADD COLUMN member_pool VARCHAR(500);
ALTER TABLE pool_member ADD CONSTRAINT pool_member_pool_once UNIQUE (sheet_id, member_pool);
